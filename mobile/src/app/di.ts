import { createHttpClient } from "@core/network/HttpClient";
import { SecureTokenStorage } from "@core/storage/TokenStorage";
import { UconApiDataSource } from "@data/datasources/UconApiDataSource";
import { AuthRepositoryImpl } from "@data/repositories/AuthRepositoryImpl";
import { MonitoringRepositoryImpl } from "@data/repositories/MonitoringRepositoryImpl";
import { PapRepositoryImpl } from "@data/repositories/PapRepositoryImpl";
import { RegistrationRepositoryImpl } from "@data/repositories/RegistrationRepositoryImpl";
import { StudentRepositoryImpl } from "@data/repositories/StudentRepositoryImpl";
import { AuthUseCases } from "@domain/usecases/AuthUseCases";
import { MonitoringUseCases } from "@domain/usecases/MonitoringUseCases";
import { PapUseCases } from "@domain/usecases/PapUseCases";
import { RegistrationUseCases } from "@domain/usecases/RegistrationUseCases";
import { StudentUseCases } from "@domain/usecases/StudentUseCases";

const tokenStorage = new SecureTokenStorage();
const httpClient = createHttpClient(tokenStorage);
const apiDataSource = new UconApiDataSource(httpClient);

export const dependencies = {
  auth: new AuthUseCases(new AuthRepositoryImpl(apiDataSource, tokenStorage)),
  registration: new RegistrationUseCases(new RegistrationRepositoryImpl(apiDataSource)),
  students: new StudentUseCases(new StudentRepositoryImpl(apiDataSource)),
  monitoring: new MonitoringUseCases(new MonitoringRepositoryImpl(apiDataSource)),
  pap: new PapUseCases(new PapRepositoryImpl(apiDataSource)),
};

export type AppDependencies = typeof dependencies;
