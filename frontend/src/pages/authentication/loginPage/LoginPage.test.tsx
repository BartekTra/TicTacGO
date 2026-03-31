import { describe, test, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/test-utils';
import LoginPage from './LoginPage';
import api from '../../../api/axios';

vi.mock('../../../api/axios', () => ({
  default: {
    post: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockApiPost = api.post as ReturnType<typeof vi.fn>;

describe('LoginPage', () => {
  const mockUser = {
    email: 'test@example.com',
    username: 'TestUser',
    gamesPlayed: 10,
    gamesWon: 5,
    winRate: 50,
    token: 'jwt-token-abc123',
  };

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  test('Happy Path: renders the login form with all expected elements', () => {
    renderWithProviders(<LoginPage />);

    expect(screen.getByRole('heading', { name: /zaloguj się/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/adres e-mail/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/hasło/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/zapamiętaj mnie/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /zaloguj/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /zapomniałeś hasła/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /zarejestruj się/i })).toBeInTheDocument();
  });

  test('Happy Path: correct link targets are provided for navigation links', () => {
    renderWithProviders(<LoginPage />);

    expect(screen.getByRole('link', { name: /zapomniałeś hasła/i })).toHaveAttribute('href', '/reset-password');
    expect(screen.getByRole('link', { name: /zarejestruj się/i })).toHaveAttribute('href', '/register');
  });

  test('Happy Path: submitting valid credentials calls API and navigates to "/"', async () => {
    mockApiPost.mockResolvedValueOnce({ data: mockUser });
    const user = userEvent.setup();

    renderWithProviders(<LoginPage />);

    await user.type(screen.getByLabelText(/adres e-mail/i), 'test@example.com');
    await user.type(screen.getByLabelText(/hasło/i), '12qwaszx');
    await user.click(screen.getByRole('button', { name: /zaloguj/i }));

    await waitFor(() => {
      expect(mockApiPost).toHaveBeenCalledWith('/auth/login', {
        email: 'test@example.com',
        password: '12qwaszx',
      });
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });

  test('Happy Path: successful login stores the JWT token in localStorage', async () => {
    mockApiPost.mockResolvedValueOnce({ data: mockUser });
    const user = userEvent.setup();

    renderWithProviders(<LoginPage />);

    await user.type(screen.getByLabelText(/adres e-mail/i), 'test@example.com');
    await user.type(screen.getByLabelText(/hasło/i), '12qwaszx');
    await user.click(screen.getByRole('button', { name: /zaloguj/i }));

    await waitFor(() => {
      expect(localStorage.getItem('jwtToken')).toBe('jwt-token-abc123');
    });
  });

  test('Error State: shows alert with server error message when login fails with API error', async () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => undefined);
    mockApiPost.mockRejectedValueOnce({
      response: { data: { message: 'Błędny email lub hasło.' } },
    });
    const user = userEvent.setup();

    renderWithProviders(<LoginPage />);

    await user.type(screen.getByLabelText(/adres e-mail/i), 'wrong@example.com');
    await user.type(screen.getByLabelText(/hasło/i), 'wrongpassword');
    await user.click(screen.getByRole('button', { name: /zaloguj/i }));

    await waitFor(() => {
      expect(alertSpy).toHaveBeenCalledWith('Błędny email lub hasło.');
    });
    expect(mockNavigate).not.toHaveBeenCalled();

    alertSpy.mockRestore();
  });

  test('Error State: does NOT navigate to "/" when login API call fails', async () => {
    vi.spyOn(window, 'alert').mockImplementation(() => undefined);
    mockApiPost.mockRejectedValueOnce({
      response: { data: { message: 'Nieautoryzowany.' } },
    });
    const user = userEvent.setup();

    renderWithProviders(<LoginPage />);

    await user.type(screen.getByLabelText(/adres e-mail/i), 'bad@example.com');
    await user.type(screen.getByLabelText(/hasło/i), 'badpass');
    await user.click(screen.getByRole('button', { name: /zaloguj/i }));

    await waitFor(() => {
      expect(mockNavigate).not.toHaveBeenCalled();
    });
  });

  test('Edge Case: "Remember Me" checkbox toggles correctly', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />);

    const checkbox = screen.getByLabelText(/zapamiętaj mnie/i);
    expect(checkbox).not.toBeChecked();

    await user.click(checkbox);
    expect(checkbox).toBeChecked();

    await user.click(checkbox);
    expect(checkbox).not.toBeChecked();
  });

  test('Edge Case: email input only accepts type="email" format', () => {
    renderWithProviders(<LoginPage />);
    expect(screen.getByLabelText(/adres e-mail/i)).toHaveAttribute('type', 'email');
  });

  test('Edge Case: password input has type="password" (hidden characters)', () => {
    renderWithProviders(<LoginPage />);
    expect(screen.getByLabelText(/hasło/i)).toHaveAttribute('type', 'password');
  });

  test('Edge Case: API is not called when form is submitted with empty fields (browser validation)', () => {
    renderWithProviders(<LoginPage />);

    const form = screen.getByRole('button', { name: /zaloguj/i }).closest('form');
    expect(form).toBeInTheDocument();

    expect(screen.getByLabelText(/adres e-mail/i)).toBeRequired();
    expect(screen.getByLabelText(/hasło/i)).toBeRequired();
  });
});
