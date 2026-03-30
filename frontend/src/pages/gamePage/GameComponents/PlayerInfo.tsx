import React from "react";

interface PlayerInfo {
  nickname: String;
  symbol: String;
}

export const PlayerInfo: React.FC<PlayerInfo> = ({ nickname, symbol }) => {
  return (
    <div className="flex flex-col items-center text-center w-50 h-25 ">
      <p> {nickname} </p>
      <p> Jest graczem: {symbol} </p>
    </div>
  );
};
