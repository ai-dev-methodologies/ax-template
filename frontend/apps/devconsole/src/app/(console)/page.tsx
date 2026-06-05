import { redirect } from 'next/navigation';

/** The console root lands on the API Keys surface. */
export default function ConsoleHome() {
  redirect('/keys');
}
