import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { useNavigate } from "react-router-dom";

export interface GameData {
  gameId: string;
  playerX: string | null;
  playerO: string | null;
  board: string;
  movesX: string;
  movesO: string;
  mode: string;
  status: string;
  currentTurn: string;
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

    const token = localStorage.getItem("jwtToken");

    const client = new Client({
      brokerURL: "ws://localhost:8080/ws-game",
      connectHeaders: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      debug: (str: string) => console.log("STOMP: " + str),
      onConnect: () => {
        console.log("Połączono z serwerem gry!");

        client.subscribe(`/topic/game.${gameId}`, (message) => {
          const data: GameData = JSON.parse(message.body);
          setGameData(data);
          setWaitingForServerState(false);
          setErrorMessage(null);

          if (data.status === "FINISHED") {
            if (intervalRef.current) clearInterval(intervalRef.current);
            if (timerRef.current) clearTimeout(timerRef.current);

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
        // wysyłamy bez ciasteczek ale z JWT Bearer
        const token = localStorage.getItem("jwtToken");
        fetch(leaveUrl, {
          method: "POST",
          keepalive: true,
          headers: { 
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {})
          },
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
      const token = localStorage.getItem("jwtToken");
      fetch(leaveUrl, {
        method: "POST",
        headers: { 
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
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
