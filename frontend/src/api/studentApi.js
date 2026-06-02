import { api } from "./client.js";

export async function listStudents() {
  const response = await api.get("/students");
  return response.data;
}

export async function getStudent(studentId) {
  const response = await api.get(`/students/${studentId}`);
  return response.data;
}

export async function getMyProfile() {
  const response = await api.get("/students/me");
  return response.data;
}

export async function getStudentGrades(studentId) {
  const response = await api.get(`/students/${studentId}/grades`);
  return response.data;
}

export async function getMyGrades() {
  const response = await api.get("/students/me/grades");
  return response.data;
}

export async function listClasses() {
  const response = await api.get("/classes");
  return response.data;
}

