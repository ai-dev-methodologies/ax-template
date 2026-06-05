import { useMutation } from '@tanstack/react-query';
import { fileClient, type StoredFile } from '@/lib/api/fileClient';

/** Upload a cover image; returns the stored-file metadata (incl. downloadUrl). */
export function useUploadCover() {
  return useMutation<StoredFile, Error, File>({
    mutationFn: (file) => fileClient.upload(file),
  });
}
