import { useEffect, useState } from "react";
import { getStudentGrades, listStudents } from "../api/studentApi.js";
import { normalizeApiError } from "../api/client.js";
import JsonPanel from "../components/JsonPanel.jsx";

export default function AdminStudents() {
  const [students, setStudents] = useState([]);
  const [selected, setSelected] = useState(null);
  const [grades, setGrades] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    listStudents()
      .then(setStudents)
      .catch((err) => setError(normalizeApiError(err)));
  }, []);

  async function selectStudent(student) {
    setSelected(student);
    try {
      setGrades(await getStudentGrades(student.studentId));
      setError(null);
    } catch (err) {
      setError(normalizeApiError(err));
    }
  }

  return (
    <div className="space-y-6">
      <header>
        <p className="text-sm uppercase tracking-[0.32em] text-clay">Admin view</p>
        <h2 className="font-display text-4xl">Student Information & Grades</h2>
      </header>

      <section className="grid gap-6 xl:grid-cols-[1fr,26rem]">
        <div className="rounded-3xl bg-paper p-6 shadow-soft">
          <h3 className="font-display text-2xl">Students</h3>
          <div className="mt-4 overflow-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-ink/50">
                <tr>
                  <th className="py-2">Student</th>
                  <th>Major</th>
                  <th>Credits</th>
                  <th>Tuition</th>
                  <th>Holds</th>
                </tr>
              </thead>
              <tbody>
                {students.map((student) => (
                  <tr
                    key={student.studentId}
                    className="cursor-pointer border-t border-ink/10 hover:bg-sand/60"
                    onClick={() => selectStudent(student)}
                  >
                    <td className="py-2">
                      <b>{student.studentId}</b>
                      <div className="text-ink/55">{student.fullName}</div>
                    </td>
                    <td>{student.major}</td>
                    <td>{student.currentCredits}</td>
                    <td>{student.tuitionPaid ? "Paid" : "Unpaid"}</td>
                    <td>{student.holds || "<empty>"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="rounded-3xl bg-paper p-6 shadow-soft">
          <h3 className="font-display text-2xl">Selected Student</h3>
          {selected ? (
            <>
              <p className="mt-3 font-semibold">{selected.studentId} - {selected.fullName}</p>
              <p className="text-sm text-ink/60">{selected.email}</p>
              <div className="mt-5 space-y-2 text-sm">
                {grades.map((grade) => (
                  <div key={grade.id} className="rounded-2xl bg-sand p-3">
                    <b>{grade.courseId}</b> {grade.courseName}
                    <div className="text-ink/60">
                      {grade.semester}: {grade.totalScore} ({grade.letterGrade})
                    </div>
                  </div>
                ))}
              </div>
            </>
          ) : <p className="mt-4 text-sm text-ink/60">Click a student to view grades.</p>}
        </div>
      </section>

      <JsonPanel title={error ? "Admin API error" : "Selected student JSON"} data={error || selected || { message: "No student selected." }} />
    </div>
  );
}

