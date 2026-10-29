/**
 * BloodBridge Vercel Serverless API Root Handler
 */
const proxyHandler = require('./[...path].js');

module.exports = async (req, res) => {
  return proxyHandler(req, res);
};
