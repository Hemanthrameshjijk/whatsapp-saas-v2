import { useEffect, useState } from 'react';
import api from '../api';
import { Check, X, Clock, Navigation, MapPin, Edit } from 'lucide-react';

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchOrders = async () => {
    try {
      const res = await api.get('/dashboard/orders?size=100');
      setOrders(res.data.content);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchOrders(); }, []);

  // Real-time updates for new orders
  useEffect(() => {
    let eventSource;
    const token = localStorage.getItem('token');
    if (token) {
      eventSource = new EventSource(`/api/dashboard/sse/events?token=${token}`);
      eventSource.addEventListener('order', (event) => {
        try {
          console.log('New order received via SSE:', event.data);
          // Refetch orders so the list updates live
          fetchOrders();
        } catch (e) {
          console.error(e);
        }
      });
      eventSource.onerror = (e) => console.warn('SSE connection error in Orders', e);
    }
    return () => eventSource && eventSource.close();
  }, []);

  const updateStatus = async (id, status) => {
    await api.patch(`/dashboard/orders/${id}/status`, { status });
    fetchOrders();
  };

  const markPaid = async (id) => {
    if (confirm('Mark this order as PAID?')) {
      await api.patch(`/dashboard/orders/${id}/payment`);
      fetchOrders();
    }
  };

  const editAddress = async (id, currentAddress) => {
    const newAddress = prompt('Edit Delivery Address:', currentAddress);
    if (newAddress !== null && newAddress !== currentAddress) {
      try {
        await api.patch(`/dashboard/orders/${id}/address`, { address: newAddress });
        fetchOrders();
      } catch (e) { console.error(e); }
    }
  };

  const getStatusBadge = (status) => {
    if (status === 'PENDING') return <span className="badge warning"><Clock size={12} style={{marginRight:'4px'}}/> Pending</span>;
    if (status === 'CONFIRMED') return <span className="badge info"><Check size={12} style={{marginRight:'4px'}}/> Confirmed</span>;
    if (status === 'OUT_FOR_DELIVERY') return <span className="badge primary"><Navigation size={12} style={{marginRight:'4px'}}/> Out for Delivery</span>;
    if (status === 'DELIVERED') return <span className="badge success"><Check size={12} style={{marginRight:'4px'}}/> Delivered</span>;
    if (status === 'CANCELLED') return <span className="badge danger"><X size={12} style={{marginRight:'4px'}}/> Cancelled</span>;
    return <span className="badge">{status}</span>;
  };

  return (
    <div>
      <h1 style={{ marginBottom: '2rem' }}>Order Management</h1>

      <div className="glass-table-container">
        {loading ? <p style={{ padding: '1rem' }}>Loading...</p> : (
          <table className="glass-table">
            <thead>
              <tr>
                <th>Order ID</th>
                <th>Customer</th>
                <th>Total</th>
                <th>Payment</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {orders.map(o => (
                <tr key={o.id}>
                  <td>
                    <strong>{o.id.split('-')[0]}</strong><br/>
                    <small style={{ color: 'var(--color-text-dim)' }}>{new Date(o.createdAt).toLocaleString()}</small>
                  </td>
                  <td>
                    <strong>{o.customerPhone}</strong><br/>
                    <div style={{ fontSize: '12px', color: 'var(--color-text-dim)', maxWidth: '200px', display: 'flex', alignItems: 'center', gap: '8px', minHeight: '20px' }}>
                      {o.deliveryType === 'PICKUP' ? (
                        <span style={{ color: 'var(--color-info)' }}>🏪 Store Pickup</span>
                      ) : (
                        o.deliveryAddressText || <span style={{ fontStyle: 'italic', opacity: 0.7 }}>No address provided</span>
                      )}
                      
                      {o.status !== 'DELIVERED' && o.status !== 'CANCELLED' && (
                        <button 
                          onClick={() => editAddress(o.id, o.deliveryAddressText || '')}
                          style={{ background: 'transparent', border: 'none', color: 'var(--color-secondary)', cursor: 'pointer', padding: 0 }}
                          title="Edit Address"
                        >
                          <Edit size={12} />
                        </button>
                      )}
                    </div>

                    {(o.deliveryLat && o.deliveryLng) && (
                      <a 
                        href={`https://www.google.com/maps?q=${o.deliveryLat},${o.deliveryLng}`} 
                        target="_blank" 
                        rel="noreferrer"
                        style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '11px', color: 'var(--color-secondary)', marginTop: '4px', textDecoration: 'none' }}
                      >
                        <MapPin size={12} /> View on Maps
                      </a>
                    )}
                  </td>
                  <td>₹{o.totalAmount}</td>
                  <td>
                    <span className={`badge ${o.paymentStatus === 'PAID' ? 'success' : 'warning'}`}>
                      {o.paymentStatus}
                    </span>
                    {o.paymentStatus !== 'PAID' && o.status !== 'CANCELLED' && (
                      <button style={{ marginLeft: '10px', background: 'transparent', border: 'none', color: 'var(--color-success)', cursor: 'pointer', textDecoration: 'underline' }} onClick={() => markPaid(o.id)}>
                        Mark Paid
                      </button>
                    )}
                  </td>
                  <td>{getStatusBadge(o.status)}</td>
                  <td>
                    <select 
                      className="glass-input" 
                      style={{ padding: '0.25rem 0.5rem', width: 'auto' }}
                      value={o.status}
                      onChange={(e) => updateStatus(o.id, e.target.value)}
                    >
                      <option value="PENDING">Pending</option>
                      <option value="CONFIRMED">Confirmed</option>
                      <option value="OUT_FOR_DELIVERY">Out for Delivery</option>
                      <option value="DELIVERED">Delivered</option>
                      <option value="CANCELLED">Cancelled</option>
                    </select>
                  </td>
                </tr>
              ))}
              {orders.length === 0 && (
                <tr><td colSpan="6" style={{ textAlign: 'center' }}>No orders yet.</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
