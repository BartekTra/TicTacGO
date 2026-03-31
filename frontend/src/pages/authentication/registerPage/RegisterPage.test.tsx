import { describe, test, expect, vi, beforeEach } from "vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithProviders } from "../../../test/test-utils";
import RegisterPage from "./RegisterPage";
import api from "../../../api/axios";

vi.mock("../../../api/axios", () => ({
  default: {
    post: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}));

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal<typeof import("react-router-dom")>();
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockApiPost = api.post as ReturnType<typeof vi.fn>;

describe("RegisterPage", () => {
  const mockUser = {
    email: "newuser@example.com",
    username: "new_user",
    gamesPlayed: 0,
    gamesWon: 0,
    winRate: 0,
    token: "jwt-register-token-xyz",
  };

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  test("Happy Path: renders the registration form with all expected elements", () => {
    renderWithProviders(<RegisterPage />);

    expect(screen.getByRole("heading", { name: /zarejestruj się/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/nazwa użytkownika/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/adres e-mail/i)).toBeInTheDocument();
    expect(screen.getByLabelText("Hasło")).toBeInTheDocument();
    expect(screen.getByLabelText(/powtórz hasło/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /zarejestruj/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /zaloguj się/i })).toBeInTheDocument();
  });

  test("Happy Path: the login link points to /login", () => {
    renderWithProviders(<RegisterPage />);
    expect(screen.getByRole("link", { name: /zaloguj się/i })).toHaveAttribute("href", "/login");
  });

  test("Happy Path: submitting valid data calls API with correct payload and navigates to /", async () => {
    mockApiPost.mockResolvedValueOnce({ data: mockUser });
    const user = userEvent.setup();

    renderWithProviders(<RegisterPage />);

    await user.type(screen.getByLabelText(/nazwa użytkownika/i), "new_user");
    await user.type(screen.getByLabelText(/adres e-mail/i), "newuser@example.com");
    await user.type(screen.getByLabelText("Hasło"), "secret123");
    await user.type(screen.getByLabelText(/powtórz hasło/i), "secret123");
    await user.click(screen.getByRole("button", { name: /zarejestruj/i }));

    await waitFor(() => {
      expect(mockApiPost).toHaveBeenCalledWith("/auth/register", {
        username: "new_user",
        email: "newuser@example.com",
        password: "secret123",
      });
      expect(mockNavigate).toHaveBeenCalledWith("/");
    });
  });

  test("Happy Path: successful registration stores the JWT token in localStorage", async () => {
    mockApiPost.mockResolvedValueOnce({ data: mockUser });
    const user = userEvent.setup();

    renderWithProviders(<RegisterPage />);

    await user.type(screen.getByLabelText(/nazwa użytkownika/i), "new_user");
    await user.type(screen.getByLabelText(/adres e-mail/i), "newuser@example.com");
    await user.type(screen.getByLabelText("Hasło"), "secret123");
    await user.type(screen.getByLabelText(/powtórz hasło/i), "secret123");
    await user.click(screen.getByRole("button", { name: /zarejestruj/i }));

    await waitFor(() => {
      expect(localStorage.getItem("jwtToken")).toBe("jwt-register-token-xyz");
    });
  });

  test("Error State: shows inline error when passwords do not match", async () => {
    const user = userEvent.setup();

    renderWithProviders(<RegisterPage />);

    await user.type(screen.getByLabelText(/nazwa użytkownika/i), "new_user");
    await user.type(screen.getByLabelText(/adres e-mail/i), "newuser@example.com");
    await user.type(screen.getByLabelText("Hasło"), "secret123");
    await user.type(screen.getByLabelText(/powtórz hasło/i), "different456");
    await user.click(screen.getByRole("button", { name: /zarejestruj/i }));

    expect(screen.getByRole("alert")).toHaveTextContent(/hasła nie są identyczne/i);
    expect(mockApiPost).not.toHaveBeenCalled();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  test("Error State: shows inline server error when username is already taken", async () => {
    mockApiPost.mockRejectedValueOnce({
      response: { data: { message: "Nazwa użytkownika jest już zajęta!" } },
    });
    const user = userEvent.setup();

    renderWithProviders(<RegisterPage />);

    await user.type(screen.getByLabelText(/nazwa użytkownika/i), "taken_user");
    await user.type(screen.getByLabelText(/adres e-mail/i), "unique@example.com");
    await user.type(screen.getByLabelText("Hasło"), "secret123");
    await user.type(screen.getByLabelText(/powtórz hasło/i), "secret123");
    await user.click(screen.getByRole("button", { name: /zarejestruj/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(
        "Nazwa użytkownika jest już zajęta!"
      );
    });
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  test("Error State: shows inline server error when email is already taken", async () => {
    mockApiPost.mockRejectedValueOnce({
      response: { data: { message: "Email jest już zajęty!" } },
    });
    const user = userEvent.setup();

    renderWithProviders(<RegisterPage />);

    await user.type(screen.getByLabelText(/nazwa użytkownika/i), "new_user");
    await user.type(screen.getByLabelText(/adres e-mail/i), "taken@example.com");
    await user.type(screen.getByLabelText("Hasło"), "secret123");
    await user.type(screen.getByLabelText(/powtórz hasło/i), "secret123");
    await user.click(screen.getByRole("button", { name: /zarejestruj/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent("Email jest już zajęty!");
    });
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  test("Error State: shows fallback message when server returns no error detail", async () => {
    mockApiPost.mockRejectedValueOnce(new Error("Network Error"));
    const user = userEvent.setup();

    renderWithProviders(<RegisterPage />);

    await user.type(screen.getByLabelText(/nazwa użytkownika/i), "new_user");
    await user.type(screen.getByLabelText(/adres e-mail/i), "newuser@example.com");
    await user.type(screen.getByLabelText("Hasło"), "secret123");
    await user.type(screen.getByLabelText(/powtórz hasło/i), "secret123");
    await user.click(screen.getByRole("button", { name: /zarejestruj/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(
        /wystąpił błąd podczas rejestracji/i
      );
    });
  });

  test("Error State: error message clears on the next successful submission attempt", async () => {
    mockApiPost
      .mockRejectedValueOnce({
        response: { data: { message: "Nazwa użytkownika jest już zajęta!" } },
      })
      .mockResolvedValueOnce({ data: mockUser });
    const user = userEvent.setup();

    renderWithProviders(<RegisterPage />);

    await user.type(screen.getByLabelText(/nazwa użytkownika/i), "taken_user");
    await user.type(screen.getByLabelText(/adres e-mail/i), "newuser@example.com");
    await user.type(screen.getByLabelText("Hasło"), "secret123");
    await user.type(screen.getByLabelText(/powtórz hasło/i), "secret123");
    await user.click(screen.getByRole("button", { name: /zarejestruj/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toBeInTheDocument();
    });

    await user.clear(screen.getByLabelText(/nazwa użytkownika/i));
    await user.type(screen.getByLabelText(/nazwa użytkownika/i), "new_user");
    await user.click(screen.getByRole("button", { name: /zarejestruj/i }));

    await waitFor(() => {
      expect(screen.queryByRole("alert")).not.toBeInTheDocument();
      expect(mockNavigate).toHaveBeenCalledWith("/");
    });
  });

  test("Edge Case: all inputs are required (browser-native validation)", () => {
    renderWithProviders(<RegisterPage />);

    expect(screen.getByLabelText(/nazwa użytkownika/i)).toBeRequired();
    expect(screen.getByLabelText(/adres e-mail/i)).toBeRequired();
    expect(screen.getByLabelText("Hasło")).toBeRequired();
    expect(screen.getByLabelText(/powtórz hasło/i)).toBeRequired();
  });

  test("Edge Case: email input enforces type=\"email\"", () => {
    renderWithProviders(<RegisterPage />);
    expect(screen.getByLabelText(/adres e-mail/i)).toHaveAttribute("type", "email");
  });

  test("Edge Case: both password inputs have type=\"password\"", () => {
    renderWithProviders(<RegisterPage />);
    expect(screen.getByLabelText("Hasło")).toHaveAttribute("type", "password");
    expect(screen.getByLabelText(/powtórz hasło/i)).toHaveAttribute("type", "password");
  });
});
