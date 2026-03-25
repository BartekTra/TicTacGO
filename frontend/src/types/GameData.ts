import type { User } from "./User";

export interface GameData {
  action: string | null;
  id: number;
  board: string;
  player1: User | null;
  player2: User | null;
  winner: User | null;
  gameMode: string;
  currentturn: User | null;
}