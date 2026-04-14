import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
});

// Interceptor to add JWT token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor to handle expired/invalid tokens
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      if (window.location.pathname !== '/login') {
        localStorage.removeItem('token');
        console.warn('Authentication failure. Redirecting to login...');
        window.location.replace('/login');
      }
    }
    return Promise.reject(error);
  }
);

export default api;
