import { NavigationContainer } from "@react-navigation/native";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { ActivityIndicator, View } from "react-native";
import { colors } from "@core/theme/theme";
import { useSession } from "@app/providers/SessionProvider";
import { RootStackParamList, MainTabParamList } from "./types";
import { AdminStudentsScreen } from "@presentation/screens/AdminStudentsScreen";
import { DashboardScreen } from "@presentation/screens/DashboardScreen";
import { DecisionTraceScreen } from "@presentation/screens/DecisionTraceScreen";
import { LoginScreen } from "@presentation/screens/LoginScreen";
import { MonitoringScreen } from "@presentation/screens/MonitoringScreen";
import { PapLifecycleScreen } from "@presentation/screens/PapLifecycleScreen";
import { PolicyExplorerScreen } from "@presentation/screens/PolicyExplorerScreen";
import { RegisterDropScreen } from "@presentation/screens/RegisterDropScreen";
import { StudentPortalScreen } from "@presentation/screens/StudentPortalScreen";
import { ValidationReportScreen } from "@presentation/screens/ValidationReportScreen";
import { StudentClassesScreen } from "@presentation/screens/student/StudentClassesScreen";
import { StudentDashboardScreen } from "@presentation/screens/student/StudentDashboardScreen";
import { StudentHistoryScreen } from "@presentation/screens/student/StudentHistoryScreen";
import { StudentRegisteredClassesScreen } from "@presentation/screens/student/StudentRegisteredClassesScreen";
import { StudentSessionsScreen } from "@presentation/screens/student/StudentSessionsScreen";

const Stack = createNativeStackNavigator<RootStackParamList>();
const Tab = createBottomTabNavigator<MainTabParamList>();

export function RootNavigator() {
  const { session, initializing } = useSession();

  if (initializing) {
    return (
      <View style={{ alignItems: "center", backgroundColor: colors.sand, flex: 1, justifyContent: "center" }}>
        <ActivityIndicator color={colors.ink} />
      </View>
    );
  }

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {session ? (
          <Stack.Screen name="Main" component={MainTabs} />
        ) : (
          <Stack.Screen name="Login" component={LoginScreen} />
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}

function MainTabs() {
  const { session } = useSession();
  const isAdmin = session?.role === "ADMIN";

  return (
    <Tab.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: colors.paper },
        headerTitleStyle: { color: colors.ink, fontWeight: "800" },
        tabBarActiveTintColor: colors.clay,
        tabBarInactiveTintColor: colors.inkSoft,
        tabBarStyle: { backgroundColor: colors.paper, borderTopColor: "rgba(16,32,26,0.1)" },
      }}
    >
      {isAdmin ? (
        <Tab.Screen name="Dashboard" component={DashboardScreen} />
      ) : (
        <Tab.Screen name="Dashboard" component={StudentDashboardScreen} options={{ title: "Dashboard" }} />
      )}
      {!isAdmin ? <Tab.Screen name="Student" component={StudentPortalScreen} options={{ title: "Profile" }} /> : null}
      {!isAdmin ? <Tab.Screen name="Classes" component={StudentClassesScreen} options={{ title: "Classes" }} /> : null}
      {!isAdmin ? <Tab.Screen name="Registered" component={StudentRegisteredClassesScreen} options={{ title: "Registered" }} /> : null}
      {!isAdmin ? <Tab.Screen name="History" component={StudentHistoryScreen} options={{ title: "History" }} /> : null}
      {!isAdmin ? <Tab.Screen name="Sessions" component={StudentSessionsScreen} options={{ title: "Sessions" }} /> : null}
      {isAdmin ? <Tab.Screen name="Admin" component={AdminStudentsScreen} options={{ title: "Students" }} /> : null}
      {isAdmin ? <Tab.Screen name="Simulator" component={RegisterDropScreen} options={{ title: "Register/Drop" }} /> : null}
      <Tab.Screen name="Trace" component={DecisionTraceScreen} options={{ title: "Trace" }} />
      {isAdmin ? <Tab.Screen name="Monitor" component={MonitoringScreen} options={{ title: "Monitor" }} /> : null}
      {isAdmin ? <Tab.Screen name="Policies" component={PolicyExplorerScreen} options={{ title: "Policies" }} /> : null}
      {isAdmin ? <Tab.Screen name="PAP" component={PapLifecycleScreen} options={{ title: "PAP" }} /> : null}
      {isAdmin ? <Tab.Screen name="Validation" component={ValidationReportScreen} options={{ title: "Validation" }} /> : null}
    </Tab.Navigator>
  );
}
