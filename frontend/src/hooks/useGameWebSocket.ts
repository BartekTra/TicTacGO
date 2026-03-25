import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  createConsumer,
  Consumer,
  Subscription,
} from "@rails/actioncable";
import { type GameData } from "../types/GameData";
import { type User } from "../types/User";

interface UseGameWebSocketReturn {
  gameData: GameData | null;
  gameBoard: string | null;
  currentTurn: User | null;
  winner: User | null;
  countdown: number | null;
}

export const useGameWebSocket = (
  gameId: string | undefined,
): UseGameWebSocketReturn => {
  const [gameData, setGameData] = useState<GameData | null>(null);
  const [gameBoard, setGameBoard] = useState<string | null>(null);
  const [currentTurn, setCurrentTurn] = useState<User | null>(null);
  const [winner, setWinner] = useState<User | null>(null);
  const [countdown, setCountdown] = useState<number | null>(null);

  const cableRef = useRef<Consumer | null>(null);
  const subscriptionRef = useRef<Subscription | null>(null);
  const timerRef = useRef<number | null>(null);
  const intervalRef = useRef<number | null>(null);

  const navigate = useNavigate();

  useEffect(() => {
    if (!gameId) return;

    const cableUrl = import.meta.env.VITE_BACKEND_WEBSOCKET_URL as string;
    const cable = createConsumer(cableUrl);
    cableRef.current = cable;

    const subscription = cable.subscriptions.create(
      { channel: "GamesChannel", id: gameId },
      {
        connected() {
          console.log("Połączono z GamesChannel");
        },
        disconnected() {
          console.log("Rozłączono z GamesChannel");
        },
        received(data: GameData) {
          console.log("Odebrano dane z kanału:", data);

          if (data.action === "please :)") {
            if (intervalRef.current) clearInterval(intervalRef.current);
            if (timerRef.current) clearTimeout(timerRef.current);

            setCountdown(3);

            timerRef.current = setTimeout(() => {
              if (subscriptionRef.current) {
                subscriptionRef.current.unsubscribe();
              }
              navigate("/");
            }, 3000);

            intervalRef.current = setInterval(() => {
              setCountdown((current) => {
                if (current === null || current <= 1) {
                  if (intervalRef.current) clearInterval(intervalRef.current);
                  return 0;
                }
                return current - 1;
              });
            }, 1000);
          } else {
            if (data.board) {
              setGameData(data);
              setGameBoard(data.board);
              console.log(gameBoard);
              if (data.currentturn) {
                setCurrentTurn(data.currentturn);
              }
            }
            if (data.winner) {
              setWinner(data.winner);
            }
          }
        },
      },
    );

    subscriptionRef.current = subscription;

    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
      }
      if (timerRef.current) clearTimeout(timerRef.current);
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [gameId, navigate]);

  return { gameData, gameBoard, currentTurn, winner, countdown };
};
