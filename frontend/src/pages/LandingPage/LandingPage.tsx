import { useEffect } from "react";
import { useMutation } from "@apollo/client/react";
import { JOIN_GAME } from "../../graphql/mutations/games/joinGame";
import type { GameData } from "../../types/GameData";
import { useNavigate } from "react-router-dom";

type JoinGameResponseType = {
  joinGame: JoinGameResponseData;
};

type JoinGameResponseData = {
  game: GameData;
  message: string;
};

type GameMode = "classic" | "infinite";

const LandingPage = () => {
  const navigate = useNavigate();
  const [joinGame, { data, loading, error }] =
    useMutation<JoinGameResponseType>(JOIN_GAME, {
      fetchPolicy: "network-only",
      onError: () => {
        console.log(error);
      },
    });

  const handleGameJoin = (gameType: GameMode) => async () => {
    try {
      await joinGame({
        variables: { gameMode: gameType },
      });
    } catch (err) {
      console.error("Błąd podczas dołączania do gry:", err);
    }
  };

  useEffect(() => {
    if (data?.joinGame.game.id) {
      navigate(`/game/${data.joinGame.game.id}`);
    }
  }, [data]);

  if (loading) return <p className="bg-gray-900">hehe</p>;

  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center font-sans">
      <div className="bg-gray-800 border border-gray-700 rounded-sm shadow-2xl p-10 w-80 flex flex-col items-center text-center">
        <h1 className="text-gray-100 text-2xl font-bold uppercase tracking-widest mb-10">
          dołącz do gry
        </h1>

        <div className="flex flex-col gap-5 w-full">
          {(["classic", "infinite"] as const).map((mode) => (
            <button
              key={mode}
              onClick={handleGameJoin(mode)}
              disabled={loading}
              className="w-full py-3 px-4 bg-gray-900 hover:bg-gray-800 text-gray-200 font-semibold uppercase tracking-wider rounded-sm transition-colors border border-gray-600 focus:outline-none disabled:opacity-50"
            >
              {loading ? "Ładowanie..." : mode}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default LandingPage;
