export type Student = {
  studentId: string;
  fullName: string;
  email: string;
  major: string;
  cohort: string;
  currentCredits: number;
  tuitionPaid: boolean;
  tuitionDebt: number;
  holds: string;
  completedCourses?: string;
  registeredClassIds?: string;
  registeredScheduleSlots?: string;
  maxCreditsEffective?: number;
  registerAttemptCount?: number;
  dropCountForSemester?: number;
  viewerRole?: string;
};

export type StudentGrade = {
  id?: number;
  studentId: string;
  courseId: string;
  courseName: string;
  semester: string;
  processScore: number;
  finalScore: number;
  totalScore: number;
  letterGrade: string;
};
