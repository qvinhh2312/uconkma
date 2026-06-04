export type MonitoringResult = {
  action?: string;
  type?: string;
  active?: boolean;
  classId?: string;
  status?: string;
  studentId?: string;
  holdCode?: string;
  checkedSessions: number;
  revokedSessions: number;
  message?: string;
};
