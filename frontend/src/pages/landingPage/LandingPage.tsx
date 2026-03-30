import { useNavigate } from "react-router-dom";
import api from "../../api/axios";

type GameMode = "classic" | "infinite";

const LandingPage = () => {
  const navigate = useNavigate();

  const handleGameJoin = (gameType: GameMode) => async () => {
    try {
      const modeParam = gameType === "classic" ? "CLASSIC" : "INFINITE";
      const { data } = await api.post(`/game/join?mode=${modeParam}`);
      navigate(`/game/${data.gameId}`, { state: { initialGameData: data } });
    } catch (err) {
      console.error("Błąd podczas dołączania do gry:", err);
      alert("Nie udało się dołączyć do gry!");
    }
  };

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
              className="w-full py-3 px-4 bg-gray-900 hover:bg-gray-800 text-gray-200 font-semibold uppercase tracking-wider rounded-sm transition-colors border border-gray-600 focus:outline-none disabled:opacity-50"
            >
              {mode}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default LandingPage;
