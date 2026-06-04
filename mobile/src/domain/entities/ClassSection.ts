export type ClassSection = {
  classId: string;
  status: string;
  capacity: number;
  enrolled: number;
  reservedSeats: number;
  scheduleSlots: string;
  semester?: string;
  courseId?: string | null;
  courseName?: string | null;
  credits?: number | null;
  tuitionFee?: number | null;
  prerequisites?: string;
};
