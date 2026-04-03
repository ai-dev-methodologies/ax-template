// Refresh queue/mutex inside auth boundary
export const refreshMutex = {
  isRefreshing: false,
  queue: []
};
