import { useEffect, useState } from "react";
import { getMyGrades, getMyProfile } from "../api/studentApi.js";
import { normalizeApiError } from "../api/client.js";
import JsonPanel from "../components/JsonPanel.jsx";

export default function StudentPortal() {
  const [profile, setProfile] = useState(null);
  const [grades, setGrades] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([getMyProfile(), getMyGrades()])
      .then(([profileData, gradeData]) => {
        setProfile(profileData);
        setGrades(gradeData);
      })
      .catch((err) => setError(normalizeApiError(err)));
  }, []);

  return (
    <div className="space-y-6">
      <header>
        <p className="text-sm uppercase tracking-[0.32em] text-clay">Student self-service</p>
        <h2 className="font-display text-4xl">My Student Portal</h2>
      </header>

      <section className="grid gap-5 xl:grid-cols-[1fr,1fr]">
        <div className="rounded-3xl bg-paper p-6 shadow-soft">
          <h3 className="font-display text-2xl">Profile</h3>
          {profile ? (
            <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
              <dt className="text-ink/55">Student ID</dt><dd className="font-semibold">{profile.studentId}</dd>
              <dt className="text-ink/55">Full name</dt><dd className="font-semibold">{profile.fullName}</dd>
              <dt className="text-ink/55">Email</dt><dd className="font-semibold">{profile.email}</dd>
              <dt className="text-ink/55">Major</dt><dd className="font-semibold">{profile.major}</dd>
              <dt className="text-ink/55">Cohort</dt><dd className="font-semibold">{profile.cohort}</dd>
              <dt className="text-ink/55">Credits</dt><dd className="font-semibold">{profile.currentCredits}</dd>
              <dt className="text-ink/55">Tuition paid</dt><dd className="font-semibold">{String(profile.tuitionPaid)}</dd>
              <dt className="text-ink/55">Tuition debt</dt><dd className="font-semibold">{profile.tuitionDebt}</dd>
              <dt className="text-ink/55">Registered classes</dt><dd className="font-semibold">{profile.registeredClassIds || "<empty>"}</dd>
              <dt className="text-ink/55">Holds</dt><dd className="font-semibold">{profile.holds || "<empty>"}</dd>
            </dl>
          ) : <p className="mt-4 text-sm text-ink/60">Loading profile...</p>}
        </div>

        <div className="rounded-3xl bg-paper p-6 shadow-soft">
          <h3 className="font-display text-2xl">Grades</h3>
          <div className="mt-4 overflow-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-ink/50">
                <tr>
                  <th className="py-2">Course</th>
                  <th>Semester</th>
                  <th>Total</th>
                  <th>Letter</th>
                </tr>
              </thead>
              <tbody>
                {grades.map((grade) => (
                  <tr key={grade.id} className="border-t border-ink/10">
                    <td className="py-2">{grade.courseId} - {grade.courseName}</td>
                    <td>{grade.semester}</td>
                    <td>{grade.totalScore}</td>
                    <td>{grade.letterGrade}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <JsonPanel title={error ? "Portal error" : "Profile JSON"} data={error || profile || { message: "Loading..." }} />
    </div>
  );
}

