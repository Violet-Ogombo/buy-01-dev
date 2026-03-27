export interface User {
  id: string;
  name: string;
  email: string;
  role: 'CLIENT' | 'SELLER';
  token: string;
  avatar?: string;
}
