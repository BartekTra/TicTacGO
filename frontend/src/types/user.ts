export interface User {
  email: string;
  username: string;
  gamesPlayed: number;
  gamesWon: number;
  winRate: number;
  token?: string;
}
