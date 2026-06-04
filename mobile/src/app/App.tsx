import { StatusBar } from "expo-status-bar";
import { DecisionProvider } from "@app/providers/DecisionProvider";
import { SessionProvider } from "@app/providers/SessionProvider";
import { RootNavigator } from "@app/navigation/RootNavigator";

export default function App() {
  return (
    <SessionProvider>
      <DecisionProvider>
        <StatusBar style="dark" />
        <RootNavigator />
      </DecisionProvider>
    </SessionProvider>
  );
}
