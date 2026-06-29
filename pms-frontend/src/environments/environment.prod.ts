export const environment = {
  production: true,
  // This gets replaced with the real backend EC2 URL during the AWS
  // deployment step — see the AWS deployment guide for exact instructions.
  apiUrl: 'http://YOUR_EC2_PUBLIC_IP:8080/api'
};

