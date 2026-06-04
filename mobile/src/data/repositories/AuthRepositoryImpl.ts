import { normalizeError } from "@core/errors/normalizeError";
import { TokenStorage } from "@core/storage/TokenStorage";
import { UconApiDataSource } from "@data/datasources/UconApiDataSource";
import { AuthSession, LoginCommand } from "@domain/entities/AuthSession";
import { AuthRepository } from "@domain/repositories/AuthRepository";

export class AuthRepositoryImpl implements AuthRepository {
  constructor(
    private readonly api: UconApiDataSource,
    private readonly tokenStorage: TokenStorage,
  ) {}

  async login(command: LoginCommand): Promise<AuthSession> {
    try {
      const session = await this.api.login(command);
      await this.tokenStorage.saveSession(session);
      return session;
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async logout(): Promise<void> {
    try {
      await this.api.logout();
    } catch {
      // Token cleanup is still required when backend session is already invalid.
    } finally {
      await this.tokenStorage.clearSession();
    }
  }

  async getStoredSession(): Promise<AuthSession | null> {
    return this.tokenStorage.getSession();
  }
}
