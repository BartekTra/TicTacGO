import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { useNavigate } from "react-router-dom";

export interface GameData {
  gameId: string;
  playerX: string;
  playerO: string | null;
  board: (string | null)[];
  currentTurn: string;
  status: string;
  winner: string;
}

export interface TicTacToeSocket {
  gameData: GameData | null;
  sendMove: (gameId: string, position: number) => void;
  countdown: number | null;
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
          console.log(data);
          console.log(message);
          if (data.status === "FINISHED") {
            if (intervalRef.current) clearInterval(intervalRef.current);
            if (timerRef.current) clearTimeout(timerRef.current);

            setCountdown(3);

            timerRef.current = setTimeout(() => {
              navigate("/");
            }, 3000);
          }
        });

        client.subscribe("/user/queue/errors", (message) => {
          alert("Niedozwolony ruch: " + message.body);
        });
      },
    });

    client.activate();
    setStompClient(client);

    return () => {
      if (client.active) client.deactivate();
    };
  }, [gameId, userId]);

  const sendMove = (gameId: string, position: number) => {
    if (!stompClient?.connected) return;

    stompClient.publish({
      destination: "/app/game.move",
      body: JSON.stringify({ gameId, position }),
    });
  };

  return { gameData, sendMove, countdown };
};
