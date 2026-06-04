import { useCallback, useState } from "react";
import { AppError } from "@core/errors/AppError";
import { normalizeError } from "@core/errors/normalizeError";

export function useAsyncAction<TArgs extends unknown[], TResult>(
  action: (...args: TArgs) => Promise<TResult>,
) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<AppError | null>(null);
  const [result, setResult] = useState<TResult | null>(null);

  const execute = useCallback(
    async (...args: TArgs) => {
      setLoading(true);
      setError(null);
      try {
        const nextResult = await action(...args);
        setResult(nextResult);
        return nextResult;
      } catch (unknownError) {
        const appError = normalizeError(unknownError);
        setError(appError);
        throw appError;
      } finally {
        setLoading(false);
      }
    },
    [action],
  );

  return { execute, loading, error, result, setResult };
}
