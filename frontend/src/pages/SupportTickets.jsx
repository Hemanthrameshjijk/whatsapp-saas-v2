import { useEffect, useState } from 'react';
import api from '../api';
import { TicketCheck, RefreshCw, Shield, Clock, CheckCircle, XCircle, AlertTriangle, Filter, Plus, Package } from 'lucide-react';

const STATUS_OPTIONS = ['OPEN', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'RESOLVED'];
const STATUS_COLORS = {
  OPEN: 'warning',
  IN_PROGRESS: 'info',
  APPROVED: 'success',
  REJECTED: 'danger',
  RESOLVED: 'primary',
};
const STATUS_ICONS = {
  OPEN: Clock,
  IN_PROGRESS: AlertTriangle,
  APPROVED: CheckCircle,
  REJECTED: XCircle,
  RESOLVED: CheckCircle,
};

export default function SupportTickets() {
  const [tickets, setTickets] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('');
  const [stats, setStats] = useState({ openTickets: 0 });
  
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ phone: '', productId: '', type: 'RETURN', orderId: '', reason: '' });

  const fetchTickets = async () => {
    try {
      const url = filter ? `/dashboard/tickets?status=${filter}&size=100` : '/dashboard/tickets?size=100';
      const res = await api.get(url);
      setTickets(res.data.content);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  };

  const fetchStats = async () => {
    try {
      const res = await api.get('/dashboard/tickets/stats');
      setStats(res.data);
    } catch (e) { console.error(e); }
  };

  const fetchProducts = async () => {
    try {
      const res = await api.get('/dashboard/products?size=100');
      setProducts(res.data.content);
    } catch (e) { console.error(e); }
  };

  useEffect(() => { fetchTickets(); fetchStats(); fetchProducts(); }, [filter]);

  const handleCreate = async () => {
    if (!form.phone || !form.productId) return alert('Phone and Product are required');
    try {
      await api.post('/dashboard/tickets', form);
      setShowForm(false);
      setForm({ phone: '', productId: '', type: 'RETURN', orderId: '', reason: '' });
      fetchTickets();
      fetchStats();
    } catch (e) { 
      console.error(e); 
      alert(e.response?.data?.message || 'Error creating ticket'); 
    }
  };

  const updateStatus = async (id, status) => {
    const adminNotes = prompt('Add admin notes (optional):');
    try {
      await api.patch(`/dashboard/tickets/${id}/status`, { status, adminNotes: adminNotes || null });
      fetchTickets();
      fetchStats();
    } catch (e) { console.error(e); }
  };

  const getTypeBadge = (type) => {
    if (type === 'RETURN') return <span className="badge info" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}><RefreshCw size={12} /> Return</span>;
    return <span className="badge primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}><Shield size={12} /> Warranty</span>;
  };

  const getStatusBadge = (status) => {
    const Icon = STATUS_ICONS[status] || Clock;
    const color = STATUS_COLORS[status] || '';
    return <span className={`badge ${color}`} style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}><Icon size={12} /> {status.replace('_', ' ')}</span>;
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <div>
          <h1 style={{ margin: 0 }}>Support Tickets</h1>
          <p style={{ color: 'var(--color-text-dim)', margin: '0.5rem 0 0 0' }}>Manage return requests and warranty claims</p>
        </div>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
          {stats.openTickets > 0 ? (
            <div className="glass-panel" style={{ padding: '0.75rem 1.25rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <TicketCheck size={18} style={{ color: 'var(--color-warning)' }} />
              <span style={{ fontWeight: 600 }}>{stats.openTickets}</span> open ticket{stats.openTickets !== 1 ? 's' : ''}
            </div>
          ) : null}
          <button className="glass-button primary" onClick={() => setShowForm(true)}>
            <Plus size={18} /> New Ticket
          </button>
        </div>
      </div>

      {/* Manual Creation Form */}
      {showForm && (
        <div className="glass-panel" style={{ padding: '1.5rem', marginBottom: '2rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
            <h3 style={{ margin: 0 }}>Create Manual Ticket</h3>
            <button onClick={() => setShowForm(false)} style={{ background: 'transparent', border: 'none', color: 'var(--color-text-dim)', cursor: 'pointer' }}>
              <XCircle size={20} />
            </button>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '1rem' }}>
            <div>
              <label className="glass-label">Customer Phone *</label>
              <input className="glass-input" placeholder="+91XXXXXXXXXX" value={form.phone}
                onChange={e => setForm({ ...form, phone: e.target.value })} />
            </div>
            <div>
              <label className="glass-label">Type</label>
              <select className="glass-input" value={form.type}
                onChange={e => setForm({ ...form, type: e.target.value })}>
                <option value="RETURN">Return Request</option>
                <option value="WARRANTY">Warranty Claim</option>
              </select>
            </div>
            <div>
              <label className="glass-label">Order ID (optional)</label>
              <input className="glass-input" placeholder="Simplified ID or UUID" value={form.orderId}
                onChange={e => setForm({ ...form, orderId: e.target.value })} />
            </div>
            <div style={{ gridColumn: 'span 2' }}>
              <label className="glass-label">Select Product *</label>
              <select className="glass-input" value={form.productId}
                onChange={e => setForm({ ...form, productId: e.target.value })}>
                <option value="">-- Choose Product --</option>
                {products.map(p => (
                  <option key={p.id} value={p.id}>{p.name} (₹{p.price})</option>
                ))}
              </select>
            </div>
            <div style={{ gridColumn: '1 / -1' }}>
              <label className="glass-label">Reason / Problem Description</label>
              <textarea className="glass-input" style={{ minHeight: '80px', resize: 'vertical' }}
                placeholder="Describe the issue..." value={form.reason}
                onChange={e => setForm({ ...form, reason: e.target.value })} />
            </div>
          </div>
          <div style={{ marginTop: '1.5rem', display: 'flex', gap: '1rem' }}>
            <button className="glass-button primary" onClick={handleCreate}>Create Ticket</button>
            <button className="glass-button" onClick={() => setShowForm(false)}>Cancel</button>
          </div>
        </div>
      )}

      {/* Filter Bar */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '1.5rem', flexWrap: 'wrap', alignItems: 'center' }}>
        <Filter size={16} style={{ color: 'var(--color-text-dim)' }} />
        <button
          className={`glass-button ${filter === '' ? 'active' : ''}`}
          style={{ padding: '0.35rem 0.75rem', fontSize: '13px' }}
          onClick={() => setFilter('')}
        >
          All
        </button>
        {STATUS_OPTIONS.map(s => (
          <button
            key={s}
            className={`glass-button ${filter === s ? 'active' : ''}`}
            style={{ padding: '0.35rem 0.75rem', fontSize: '13px' }}
            onClick={() => setFilter(s)}
          >
            {s.replace('_', ' ')}
          </button>
        ))}
      </div>

      <div className="glass-table-container">
        {loading ? <p style={{ padding: '1rem' }}>Loading...</p> : (
          <table className="glass-table">
            <thead>
              <tr>
                <th>Ticket</th>
                <th>Customer</th>
                <th>Type</th>
                <th>Product</th>
                <th>Reason</th>
                <th>Policy</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map(t => (
                <tr key={t.id}>
                  <td>
                    <strong>{t.id.split('-')[0]}</strong><br/>
                    <small style={{ color: 'var(--color-text-dim)' }}>{new Date(t.createdAt).toLocaleString()}</small>
                  </td>
                  <td>
                    <strong>{t.customerPhone}</strong>
                  </td>
                  <td>{getTypeBadge(t.type)}</td>
                  <td>
                    <strong>{t.productName || '—'}</strong>
                  </td>
                  <td>
                    <div style={{ maxWidth: '200px', fontSize: '13px' }}>{t.reason}</div>
                  </td>
                  <td>
                    <div style={{ maxWidth: '180px', fontSize: '12px', color: 'var(--color-text-dim)' }}>
                      {t.policyApplied || '—'}
                    </div>
                  </td>
                  <td>{getStatusBadge(t.status)}</td>
                  <td>
                    <select
                      className="glass-input"
                      style={{ padding: '0.25rem 0.5rem', width: 'auto', fontSize: '12px' }}
                      value={t.status}
                      onChange={(e) => updateStatus(t.id, e.target.value)}
                    >
                      {STATUS_OPTIONS.map(s => (
                        <option key={s} value={s}>{s.replace('_', ' ')}</option>
                      ))}
                    </select>
                    {t.adminNotes && (
                      <div style={{ fontSize: '11px', color: 'var(--color-text-dim)', marginTop: '4px', maxWidth: '150px' }}>
                        📝 {t.adminNotes}
                      </div>
                    )}
                  </td>
                </tr>
              ))}
              {tickets.length === 0 && (
                <tr><td colSpan="8" style={{ textAlign: 'center', padding: '2rem' }}>
                  <TicketCheck size={40} style={{ opacity: 0.3, marginBottom: '1rem' }} /><br/>
                  No tickets found.
                </td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
