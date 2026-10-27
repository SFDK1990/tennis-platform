"use client";

import { useStudentPhoto } from "@/modules/student/api";
import { Avatar } from "@/shared/ui/Avatar";

/** A managed student's face for the teacher: the photo when there is one, the initials meanwhile. */
export function StudentAvatar({ userId, name, hasPhoto, size }: {
  userId: string; name: string | null; hasPhoto: boolean; size?: "sm" | "lg";
}) {
  const photo = useStudentPhoto(userId, hasPhoto);
  return <Avatar name={name} src={hasPhoto ? photo.data : null} size={size} />;
}
