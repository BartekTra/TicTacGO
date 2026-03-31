import { describe, test, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../test/test-utils';
import GamePage from './GamePage';
import { useTicTacToeSocket } from './GameComponents/hooks/useGameWebSocket';

vi.mock('./GameComponents/hooks/useGameWebSocket', () => ({
  useTicTacToeSocket: vi.fn(),
}));

const mockUseSocket = useTicTacToeSocket as ReturnType<typeof vi.fn>;

describe('GamePage', () => {
  const defaultUser = { email: 'test@example.com', nickname: 'TestUser', rank: 1000 };
  const mockSendMove = vi.fn();
  const mockLeaveGame = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  const setupSocketMock = (overrides = {}) => {
    mockUseSocket.mockReturnValue({
      gameData: {
        gameId: '123',
        playerX: 'test@example.com',
        playerO: 'opponent@example.com',
        board: '---------',
        movesX: '',
        movesO: '',
        mode: 'CLASSIC',
        status: 'IN_PROGRESS',
        currentTurn: 'X',
        winner: null,
      },
      sendMove: mockSendMove,
      countdown: null,
      waitingForServerState: false,
      errorMessage: null,
      leaveGame: mockLeaveGame,
      ...overrides
    });
  };

  test('Loading State: shows "Loading session..." when user or gameId is not available', () => {
    renderWithProviders(<GamePage />, { 
      route: '/game/123',
      path: '/game/:gameId',
      preloadedState: { auth: { user: null, isAuthenticated: false } }
    });
    
    expect(screen.getByText(/loading session/i)).toBeInTheDocument();
  });

  test('Loading State: shows "Ładowanie gry..." when gameData is missing from socket', () => {
    mockUseSocket.mockReturnValue({
      gameData: null,
      sendMove: mockSendMove,
      countdown: null,
      waitingForServerState: false,
      errorMessage: null,
      leaveGame: mockLeaveGame
    });

    renderWithProviders(<GamePage />, {
      route: '/game/123',
      path: '/game/:gameId',
      preloadedState: { auth: { user: defaultUser, isAuthenticated: true } }
    });
    
    expect(screen.getByText(/Ładowanie gry/i)).toBeInTheDocument();
  });

  test('Happy Path: renders the game board, players, and allows sending a move', async () => {
    setupSocketMock();
    const user = userEvent.setup();

    renderWithProviders(<GamePage />, {
      route: '/game/123',
      path: '/game/:gameId',
      preloadedState: { auth: { user: defaultUser, isAuthenticated: true } },
    });

    expect(screen.getByRole('button', { name: /opuść grę/i })).toBeInTheDocument();
    
    expect(screen.getByText('test@example.com')).toBeInTheDocument(); 
    expect(screen.getByText('opponent@example.com')).toBeInTheDocument(); 

    const cells = screen.getAllByRole('button').filter(b => b.textContent !== 'Opuść grę');
    
    expect(cells).toHaveLength(9);
    
    expect(cells[0]).not.toBeDisabled();
    
    await user.click(cells[0]);

    expect(mockSendMove).toHaveBeenCalledWith('123', 0);
  });

  test('Error States: displays errorMessage correctly', () => {
    setupSocketMock({ errorMessage: 'Niedozwolony ruch!' });

    renderWithProviders(<GamePage />, {
      route: '/game/123',
      path: '/game/:gameId',
      preloadedState: { auth: { user: defaultUser, isAuthenticated: true } }
    });

    expect(screen.getByText('Niedozwolony ruch!')).toBeInTheDocument();
  });

  test('Edge Cases: Leave game button triggers leaveGame', async () => {
    setupSocketMock();
    const user = userEvent.setup();

    renderWithProviders(<GamePage />, {
      route: '/game/123',
      path: '/game/:gameId',
      preloadedState: { auth: { user: defaultUser, isAuthenticated: true } }
    });

    const leaveButton = screen.getByRole('button', { name: /opuść grę/i });
    await user.click(leaveButton);
    expect(mockLeaveGame).toHaveBeenCalledTimes(1);
  });

  test('Edge Cases: prevents move execution if it is not my turn', async () => {
    setupSocketMock({ 
      gameData: {
        gameId: '123',
        playerX: 'test@example.com',
        playerO: 'opponent@example.com',
        board: '---------',
        movesX: '',
        movesO: '',
        mode: 'CLASSIC',
        status: 'IN_PROGRESS',
        currentTurn: 'O',
        winner: null,
      }
    });
    
    renderWithProviders(<GamePage />, {
      route: '/game/123',
      path: '/game/:gameId',
      preloadedState: { auth: { user: defaultUser, isAuthenticated: true } }
    });

    const cells = screen.getAllByRole('button').filter(b => b.textContent !== 'Opuść grę');
    expect(cells[0]).toBeDisabled();
  });
});
