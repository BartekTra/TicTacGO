import { useEffect, useState, type ReactNode } from "react";

interface PlayerTimerWrapperProps {
  children: ReactNode;
  isActive: boolean;
  duration?: number;
}

const PlayerTimerWrapper = ({ 
  children, 
  isActive, 
  duration = 15 
}: PlayerTimerWrapperProps) => {
  const [key, setKey] = useState(0);

  useEffect(() => {
    if (isActive) {
      setKey((prev) => prev + 1);
    }
  }, [isActive]);

  return (
    <div className="relative p-4 rounded-sm bg-gray-800 flex items-center justify-center">
      {isActive && (
        <svg
          key={key}
          className="absolute inset-0 w-full h-full pointer-events-none"
          xmlns="http://www.w3.org/2000/svg"
        >
          <defs>
            <filter id="fire-glow" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="4" result="blur" />
              <feMerge>
                <feMergeNode in="blur" />
                <feMergeNode in="blur" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
          </defs>

          <rect
            x="2"
            y="2"
            width="calc(100% - 4px)"
            height="calc(100% - 4px)"
            fill="none"
            stroke="#ff5e00"
            strokeWidth="4"
            rx="2"
            pathLength="100"
            strokeDasharray="100"
            strokeDashoffset="0"
            filter="url(#fire-glow)"
            style={{
              animation: `draw-timer ${duration}s linear forwards`,
            }}
          />
        </svg>
      )}

      <div className="relative z-10">
        {children}
      </div>
    </div>
  );
};

export default PlayerTimerWrapper;