import { useEffect, useState } from 'react';
import api from '../api';
import { ShieldAlert, ShieldCheck, MapPin, CreditCard, History, User, XCircle, Copy, ExternalLink, TicketCheck, Users } from 'lucide-react';

export default function Customers() {
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedPhone, setSelectedPhone] = useState(null);
  const [details, setDetails] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(false);

  const fetchCustomers = async () => {
    try {
      const res = await api.get('/dashboard/customers?size=100');
      setCustomers(res.data.content);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchCustomers(); }, []);

  const fetchDetails = async (phone) => {
    setDetailsLoading(true);
    setSelectedPhone(phone);
    try {
      const res = await api.get(`/dashboard/customers/${phone}/details`);
      setDetails(res.data);
    } catch (e) { console.error(e); }
    finally { setDetailsLoading(false); }
  };

  const closeDetails = () => {
    setSelectedPhone(null);
    setDetails(null);
  };

  const toggleBlock = async (phone, currentBlocked) => {
    const action = currentBlocked ? 'Unblock' : 'Block';
    if (confirm(`Are you sure you want to ${action} ${phone}?`)) {
      await api.patch(`/dashboard/customers/${phone}/block`, { blocked: !currentBlocked });
      fetchCustomers();
    }
  };

  return (
    <div>
      <h1 style={{ marginBottom: '2rem' }}>Customer Intelligence</h1>

      <div className="glass-table-container">
        {loading ? <p style={{ padding: '1rem' }}>Loading...</p> : (
          <table className="glass-table">
            <thead>
              <tr>
                <th>Customer Name</th>
                <th>Phone Number</th>
                <th>Total Orders</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {customers.map(c => (
                <tr key={c.id}>
                  <td><strong>{c.name || 'Unknown'}</strong></td>
                  <td>{c.phoneNumber}</td>
                  <td>{c.totalOrders}</td>
                  <td>
                    {c.isBlocked ? (
                      <span className="badge danger"><ShieldAlert size={12} style={{marginRight:'4px'}}/> Blocked</span>
                    ) : (
                      <span className="badge success"><ShieldCheck size={12} style={{marginRight:'4px'}}/> Active</span>
                    )}
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <button 
                        className="glass-button" 
                        style={{ padding: '0.25rem 0.75rem', fontSize: '0.8rem', background: 'var(--color-primary)' }}
                        onClick={() => fetchDetails(c.phoneNumber)}
                      >
                        Details
                      </button>
                      <button 
                        className={`glass-button ${c.isBlocked ? '' : 'danger'}`} 
                        style={{ padding: '0.25rem 0.75rem', fontSize: '0.8rem' }}
                        onClick={() => toggleBlock(c.phoneNumber, c.isBlocked)}
                      >
                        {c.isBlocked ? 'Unblock' : 'Block'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {customers.length === 0 && (
                <tr><td colSpan="5" style={{ textAlign: 'center' }}>No customers yet.</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>

      {/* Customer Detail Overlay */}
      {selectedPhone && (
        <div className="glass-panel" style={{
          position: 'fixed', top: '10vh', right: '2rem', bottom: '10vh', width: '600px',
          zIndex: 100, display: 'flex', flexDirection: 'column', boxSizing: 'border-box',
          animation: 'slideIn 0.3s ease-out'
        }}>
          <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--color-glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h2 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <User size={24} /> Customer Profile
            </h2>
            {details?.customer?.referredBy && (
              <div className="badge success" style={{ padding: '6px 12px', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Users size={14} /> Referred by: {details.customer.referredBy}
              </div>
            )}
            <button onClick={closeDetails} style={{ background: 'transparent', border: 'none', color: 'var(--color-text-dim)', cursor: 'pointer' }}>
              <XCircle size={24} />
            </button>
          </div>

          <div style={{ flex: 1, overflowY: 'auto', padding: '1.5rem' }}>
            {detailsLoading ? <p>Loading customer intelligence...</p> : details && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                
                {/* Profile Stats */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                  <div className="glass-card" style={{ padding: '1rem' }}>
                    <small style={{ color: 'var(--color-text-dim)', display: 'block', marginBottom: '4px' }}>LIFETIME VALUE</small>
                    <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--color-secondary)' }}>
                      ₹{details.totalSpent.toLocaleString()}
                    </div>
                  </div>
                  <div className="glass-card" style={{ padding: '1rem' }}>
                    <small style={{ color: 'var(--color-text-dim)', display: 'block', marginBottom: '4px' }}>TOTAL ORDERS</small>
                    <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>
                      {details.orders.length}
                    </div>
                  </div>
                </div>

                {/* Delivery Info */}
                <div className="glass-card">
                  <h4 style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <MapPin size={18} /> Delivery Intelligence
                  </h4>
                  <p style={{ fontSize: '14px', marginBottom: '1rem' }}>
                    {details.customer.lastDeliveryAddress || 'No delivery address saved yet.'}
                  </p>
                  {details.customer.lastLat && (
                    <div style={{ display: 'flex', gap: '1rem', color: 'var(--color-text-dim)', fontSize: '12px' }}>
                      <span>Lat: {details.customer.lastLat.toFixed(4)}</span>
                      <span>Lng: {details.customer.lastLng.toFixed(4)}</span>
                      <a href={`https://www.google.com/maps?q=${details.customer.lastLat},${details.customer.lastLng}`} 
                         target="_blank" rel="noreferrer" style={{ color: 'var(--color-secondary)', textDecoration: 'none' }}>
                        View on Map <ExternalLink size={10} />
                      </a>
                    </div>
                  )}
                </div>

                {/* Order History */}
                <div>
                  <h4 style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <History size={18} /> Order History
                  </h4>
                  <div className="glass-table-container" style={{ margin: 0, maxHeight: '300px' }}>
                    <table className="glass-table">
                      <thead>
                        <tr>
                          <th>Order</th>
                          <th>Date</th>
                          <th>Total</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {details.orders.map(o => (
                          <tr key={o.id}>
                            <td><small>{o.id.split('-')[0]}</small></td>
                            <td><small>{new Date(o.createdAt).toLocaleDateString()}</small></td>
                            <td>₹{o.totalAmount}</td>
                            <td><small className={`badge ${o.status === 'DELIVERED' ? 'success' : 'warning'}`}>{o.status}</small></td>
                          </tr>
                        ))}
                        {details.orders.length === 0 && <tr><td colSpan="4" style={{ textAlign: 'center' }}>No orders yet.</td></tr>}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* Complaint History */}
                <div>
                  <h4 style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <TicketCheck size={18} /> Support & Complaints
                  </h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                    {details.tickets.map(t => (
                      <div key={t.id} className="glass-panel" style={{ padding: '0.75rem', fontSize: '13px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                          <span className={`badge ${t.type === 'RETURN' ? 'info' : 'primary'}`}>{t.type}</span>
                          <span style={{ color: 'var(--color-text-dim)' }}>{new Date(t.createdAt).toLocaleDateString()}</span>
                        </div>
                        <strong>{t.productName}</strong>
                        <p style={{ margin: '4px 0', fontSize: '12px' }}>{t.reason}</p>
                        <small className={`badge ${t.status === 'RESOLVED' ? 'success' : 'warning'}`}>{t.status}</small>
                      </div>
                    ))}
                    {details.tickets.length === 0 && <p style={{ fontSize: '13px', color: 'var(--color-text-dim)' }}>No support tickets or complaints.</p>}
                  </div>
                </div>

              </div>
            )}
          </div>
        </div>
      )}

      {/* Animation Styles */}
      <style>{`
        @keyframes slideIn {
          from { transform: translateX(100px); opacity: 0; }
          to { transform: translateX(0); opacity: 1; }
        }
      `}</style>
    </div>
  );
}
