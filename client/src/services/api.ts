import axios from 'axios';
import toast from 'react-hot-toast';

const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const message = error.response?.data?.message || 'Something went wrong';

    if (status === 401) {
      // Clear auth state will be handled by the store listener
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
    } else if (status === 403) {
      toast.error('Access denied');
    } else if (status >= 500) {
      toast.error('Server error — try again later');
    }

    return Promise.reject({ status, message });
  }
);

export default api;
