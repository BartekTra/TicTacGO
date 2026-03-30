import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { useNavigate } from "react-router-dom";

export interface GameData {
  gameId: string;
  playerX: string | null;
  playerO: string | null;
  board: (string | null)[];
  currentTurn: string;
  status: string;
  winner: string | null;
}

export interface TicTacToeSocket {
  gameData: GameData | null;
  sendMove: (gameId: string, position: number) => void;
  countdown: number | null;
  waitingForServerState: boolean;
  errorMessage: string | null;
  leaveGame: () => void;
}

export const useTicTacToeSocket = (
  gameId: string,
  userId: string | undefined,
  initialGameData: GameData | null = null,
): TicTacToeSocket => {
  const [gameData, setGameData] = useState<GameData | null>(initialGameData);
  const [stompClient, setStompClient] = useState<Client | null>(null);
  const timerRef = useRef<number | null>(null);
  const intervalRef = useRef<number | null>(null);
  const [countdown, setCountdown] = useState<number | null>(null);
  const [waitingForServerState, setWaitingForServerState] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const skipLeaveRef = useRef(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (!gameId || !userId) return;

    const client = new Client({
      brokerURL: "ws://localhost:8080/ws-game",
      debug: (str: string) => console.log("STOMP: " + str),
      onConnect: () => {
        console.log("Połączono z serwerem gry!");

        client.subscribe(`/topic/game.${gameId}`, (message) => {
          const data: GameData = JSON.parse(message.body);
          setGameData(data);
          // Po nowym stanie z backendu odblokowujemy UI.
          setWaitingForServerState(false);
          setErrorMessage(null);

          if (data.status === "FINISHED") {
            if (intervalRef.current) clearInterval(intervalRef.current);
            if (timerRef.current) clearTimeout(timerRef.current);

            // Nie wysyłamy "leave" podczas automatycznego przekierowania.
            skipLeaveRef.current = true;

            setCountdown(3);
            intervalRef.current = window.setInterval(() => {
              setCountdown((prev) => {
                if (prev === null) return prev;
                if (prev <= 1) {
                  if (intervalRef.current) clearInterval(intervalRef.current);
                  intervalRef.current = null;
                  navigate("/");
                  return 0;
                }
                return prev - 1;
              });
            }, 1000);

            // Zakończmy nasłuch, żeby nie wysyłać zbędnych eventów.
            if (client.active) client.deactivate();
          }
        });

        client.subscribe("/user/queue/errors", (message) => {
          const raw = message.body ?? "";
          const body = String(raw);

          const lower = body.toLowerCase();
          const isOptimisticLockConflict =
            lower.includes("konflikt") && (lower.includes("współbie") || lower.includes("wspolnie"));

          if (isOptimisticLockConflict) {
            setErrorMessage("Ktoś wykonał ruch szybciej. Odświeżam stan gry...");
            setWaitingForServerState(true);
          } else {
            setErrorMessage(body ? `Niedozwolony ruch: ${body}` : "Niedozwolony ruch.");
            // Przy innych błędach nie blokujemy długoterminowo UI.
            setWaitingForServerState(false);
          }
        });
      },
    });

    client.activate();
    setStompClient(client);

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
      if (timerRef.current) clearTimeout(timerRef.current);
      if (client.active) client.deactivate();
    };
  }, [gameId, userId]);

  useEffect(() => {
    if (!gameId || !userId) return;

    const leaveUrl = `${import.meta.env.VITE_API_ADDRESS}/game/leave?gameId=${encodeURIComponent(
      gameId
    )}`;

    const onBeforeUnload = () => {
      if (skipLeaveRef.current) return;
      try {
        // `keepalive` + `credentials: include` maksymalizuje szansę wysłania requesta podczas zamykania karty.
        fetch(leaveUrl, {
          method: "POST",
          keepalive: true,
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: "",
        }).catch(() => undefined);
      } catch {
        // Ignorujemy błędy, bo to tylko clean-up.
      }
    };

    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, [gameId, userId]);

  const leaveGame = () => {
    skipLeaveRef.current = true;
    if (gameId && userId) {
      const leaveUrl = `${import.meta.env.VITE_API_ADDRESS}/game/leave?gameId=${encodeURIComponent(
        gameId
      )}`;
      fetch(leaveUrl, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: "",
      }).catch(() => undefined);
    }
    navigate("/");
  };

  const sendMove = (gameId: string, position: number) => {
    if (!stompClient?.connected) return;
    if (waitingForServerState) return;
    // Blokujemy UI aż do przyjścia kolejnego stanu z backendu.
    setWaitingForServerState(true);
    setErrorMessage(null);

    stompClient.publish({
      destination: "/app/game.move",
      body: JSON.stringify({ gameId, position }),
    });
  };

  return {
    gameData,
    sendMove,
    countdown,
    waitingForServerState,
    errorMessage,
    leaveGame,
  };
};
