import { useEffect, useState } from 'react';
import api from '../api';
import { Tag, Plus, Trash2, Edit, ToggleLeft, ToggleRight, Clock, Users, Package, ShoppingCart, Zap } from 'lucide-react';

const EMPTY_PROMO = {
  code: '', description: '', discountType: 'PERCENTAGE', discountValue: '',
  maxDiscount: '', minOrderAmount: '', productId: '', customerPhone: '',
  firstOrderOnly: false, maxUses: '', isActive: true, startsAt: '', expiresAt: '',
  autoApply: false, offerLabel: '', category: '', brand: ''
};

export default function Promotions() {
  const [promos, setPromos] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ ...EMPTY_PROMO });

  const fetchPromosAndProducts = async () => {
    try {
      const [promosRes, productsRes] = await Promise.all([
        api.get('/dashboard/promotions?size=100'),
        api.get('/dashboard/products?size=100')
      ]);
      setPromos(promosRes.data.content);
      setProducts(productsRes.data.content);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchPromosAndProducts(); }, []);

  const handleChange = (field, value) => setForm(f => ({ ...f, [field]: value }));

  const openCreate = () => {
    setEditing(null);
    setForm({ ...EMPTY_PROMO });
    setShowForm(true);
  };

  const openEdit = (p) => {
    setEditing(p.id);
    setForm({
      code: p.code || '', description: p.description || '',
      discountType: p.discountType || 'PERCENTAGE', discountValue: p.discountValue || '',
      maxDiscount: p.maxDiscount || '', minOrderAmount: p.minOrderAmount || '',
      productId: p.productId || '', customerPhone: p.customerPhone || '',
      firstOrderOnly: p.firstOrderOnly || false, maxUses: p.maxUses || '',
      isActive: p.isActive !== false,
      startsAt: p.startsAt ? p.startsAt.substring(0, 16) : '',
      expiresAt: p.expiresAt ? p.expiresAt.substring(0, 16) : '',
      autoApply: p.autoApply || false,
      offerLabel: p.offerLabel || '',
      category: p.category || '',
      brand: p.brand || ''
    });
    setShowForm(true);
  };

  const handleSave = async () => {
    const payload = {
      ...form,
      discountValue: parseFloat(form.discountValue) || 0,
      maxDiscount: form.maxDiscount ? parseFloat(form.maxDiscount) : null,
      minOrderAmount: form.minOrderAmount ? parseFloat(form.minOrderAmount) : null,
      productId: form.productId || null,
      customerPhone: form.customerPhone || null,
      maxUses: form.maxUses ? parseInt(form.maxUses) : null,
      startsAt: form.startsAt || null,
      expiresAt: form.expiresAt || null,
    };
    try {
      if (editing) {
        await api.put(`/dashboard/promotions/${editing}`, payload);
      } else {
        await api.post('/dashboard/promotions', payload);
      }
      setShowForm(false);
      fetchPromosAndProducts();
    } catch (e) { console.error(e); alert('Error saving promotion'); }
  };

  const handleDelete = async (id) => {
    if (confirm('Delete this promotion?')) {
      await api.delete(`/dashboard/promotions/${id}`);
      fetchPromosAndProducts();
    }
  };

  const toggleActive = async (p) => {
    await api.put(`/dashboard/promotions/${p.id}`, { ...p, isActive: !p.isActive });
    fetchPromosAndProducts();
  };

  const isExpired = (p) => p.expiresAt && new Date(p.expiresAt) < new Date();
  const isExhausted = (p) => p.maxUses && p.usedCount >= p.maxUses;

  const getConditionBadges = (p) => {
    const badges = [];
    if (p.productId) {
      const prod = products.find(pr => pr.id === p.productId);
      badges.push(
        <span key="p" className="badge info" style={{ fontSize: '10px', padding: '2px 6px' }}>
          <Package size={10} /> {prod ? prod.name : 'Product'}
        </span>
      );
    }
    if (p.category) badges.push(<span key="c" className="badge" style={{ fontSize: '10px', padding: '2px 6px' }}>📂 {p.category}</span>);
    if (p.brand) badges.push(<span key="b" className="badge primary" style={{ fontSize: '10px', padding: '2px 6px' }}>🏅 {p.brand}</span>);
    if (p.autoApply) badges.push(<span key="a" className="badge success" style={{ fontSize: '10px', padding: '2px 6px' }}><Zap size={10} /> Auto</span>);
    if (p.customerPhone) badges.push(<span key="u" className="badge primary" style={{ fontSize: '10px', padding: '2px 6px' }}><Users size={10} /> User</span>);
    if (p.firstOrderOnly) badges.push(<span key="f" className="badge warning" style={{ fontSize: '10px', padding: '2px 6px' }}>1st Order</span>);
    if (p.minOrderAmount) badges.push(<span key="m" className="badge" style={{ fontSize: '10px', padding: '2px 6px' }}><ShoppingCart size={10} /> Min ₹{p.minOrderAmount}</span>);
    return badges;
  };

  // Get unique categories and brands from products
  const productCategories = [...new Set(products.map(p => p.category).filter(Boolean))];
  const productBrands = [...new Set(products.map(p => p.brand).filter(Boolean))];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <div>
          <h1 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Tag size={28} /> Promotions & Offers
          </h1>
          <p style={{ color: 'var(--color-text-dim)', margin: '0.5rem 0 0 0' }}>Create promo codes (manual) & offers (auto-apply)</p>
        </div>
        <button className="glass-button primary" onClick={openCreate}>
          <Plus size={16} /> New Promo
        </button>
      </div>

      {/* Create/Edit Form */}
      {showForm && (
        <div className="glass-panel" style={{ padding: '1.5rem', marginBottom: '2rem' }}>
          <h3 style={{ marginTop: 0 }}>{editing ? 'Edit Promotion' : 'Create New Promotion'}</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '1rem' }}>
            <div>
              <label className="glass-label">Promo Code *</label>
              <input className="glass-input" placeholder="SUMMER20" value={form.code}
                onChange={e => handleChange('code', e.target.value.toUpperCase())} />
            </div>
            <div>
              <label className="glass-label">Discount Type *</label>
              <select className="glass-input" value={form.discountType}
                onChange={e => handleChange('discountType', e.target.value)}>
                <option value="PERCENTAGE">Percentage (%)</option>
                <option value="FLAT">Flat Amount (₹)</option>
              </select>
            </div>
            <div>
              <label className="glass-label">Discount Value *</label>
              <input className="glass-input" type="number" placeholder={form.discountType === 'PERCENTAGE' ? '20' : '100'}
                value={form.discountValue} onChange={e => handleChange('discountValue', e.target.value)} />
            </div>
            <div style={{ gridColumn: '1 / -1' }}>
              <label className="glass-label">Description</label>
              <input className="glass-input" placeholder="Summer sale - 20% off everything"
                value={form.description} onChange={e => handleChange('description', e.target.value)} />
            </div>
            <div>
              <label className="glass-label">Max Discount Cap (₹)</label>
              <input className="glass-input" type="number" placeholder="200 (optional)"
                value={form.maxDiscount} onChange={e => handleChange('maxDiscount', e.target.value)} />
            </div>
            <div>
              <label className="glass-label">Min Order Amount (₹)</label>
              <input className="glass-input" type="number" placeholder="500 (optional)"
                value={form.minOrderAmount} onChange={e => handleChange('minOrderAmount', e.target.value)} />
            </div>
            <div>
              <label className="glass-label">Max Uses</label>
              <input className="glass-input" type="number" placeholder="100 (optional)"
                value={form.maxUses} onChange={e => handleChange('maxUses', e.target.value)} />
            </div>
            <div>
              <label className="glass-label">Specific Product ID (optional)</label>
              <select className="glass-input" value={form.productId}
                onChange={e => handleChange('productId', e.target.value)}>
                <option value="">Any Product (None Selected)</option>
                {products.map(prod => (
                  <option key={prod.id} value={prod.id}>
                    {prod.name} (₹{prod.price})
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="glass-label">Specific Customer Phone (optional)</label>
              <input className="glass-input" placeholder="+91XXXXXXXXXX"
                value={form.customerPhone} onChange={e => handleChange('customerPhone', e.target.value)} />
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', paddingTop: '1.5rem' }}>
              <input type="checkbox" id="firstOrder" checked={form.firstOrderOnly}
                onChange={e => handleChange('firstOrderOnly', e.target.checked)} />
              <label htmlFor="firstOrder">First Order Only</label>
            </div>
            <div>
              <label className="glass-label">Starts At</label>
              <input className="glass-input" type="datetime-local"
                value={form.startsAt} onChange={e => handleChange('startsAt', e.target.value)} />
            </div>
            <div>
              <label className="glass-label">Expires At</label>
              <input className="glass-input" type="datetime-local"
                value={form.expiresAt} onChange={e => handleChange('expiresAt', e.target.value)} />
            </div>

            {/* Offer-specific fields */}
            <div style={{ gridColumn: '1 / -1', borderTop: '1px solid var(--color-glass-border)', paddingTop: '1rem', marginTop: '0.5rem' }}>
              <h4 style={{ margin: '0 0 0.75rem 0', color: 'var(--color-secondary)', display: 'flex', alignItems: 'center', gap: '6px' }}><Zap size={16} /> Auto-Apply Offer Settings</h4>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <input type="checkbox" id="autoApply" checked={form.autoApply}
                onChange={e => handleChange('autoApply', e.target.checked)} />
              <label htmlFor="autoApply">Auto-Apply (no code needed)</label>
            </div>
            <div>
              <label className="glass-label">Offer Label</label>
              <input className="glass-input" placeholder="🔥 10% OFF on Electronics"
                value={form.offerLabel} onChange={e => handleChange('offerLabel', e.target.value)} />
            </div>
            <div>
              <label className="glass-label">Product Category</label>
              <input className="glass-input" placeholder="Electronics, Clothing, etc."
                value={form.category} onChange={e => handleChange('category', e.target.value)}
                list="category-options" />
              <datalist id="category-options">
                {productCategories.map(cat => (
                  <option key={cat} value={cat} />
                ))}
              </datalist>
            </div>
            <div>
              <label className="glass-label">Product Brand</label>
              <input className="glass-input" placeholder="Nike, Samsung, etc."
                value={form.brand} onChange={e => handleChange('brand', e.target.value)}
                list="brand-options" />
              <datalist id="brand-options">
                {productBrands.map(br => (
                  <option key={br} value={br} />
                ))}
              </datalist>
            </div>
          </div>
          <div style={{ marginTop: '1.5rem', display: 'flex', gap: '1rem' }}>
            <button className="glass-button primary" onClick={handleSave}>
              {editing ? 'Update' : 'Create'} Promotion
            </button>
            <button className="glass-button" onClick={() => setShowForm(false)}>Cancel</button>
          </div>
        </div>
      )}

      {/* Promotions Table */}
      <div className="glass-table-container">
        {loading ? <p style={{ padding: '1rem' }}>Loading...</p> : (
          <table className="glass-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Description</th>
                <th>Discount</th>
                <th>Conditions</th>
                <th>Usage</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {promos.map(p => (
                <tr key={p.id} style={{ opacity: (!p.isActive || isExpired(p) || isExhausted(p)) ? 0.5 : 1 }}>
                  <td>
                    <strong style={{ fontSize: '14px', letterSpacing: '1px' }}>{p.code}</strong>
                    {p.autoApply && <div style={{ fontSize: '10px', color: 'var(--color-success)', marginTop: '2px' }}>⚡ Auto-Apply</div>}
                    {p.offerLabel && <div style={{ fontSize: '11px', color: 'var(--color-secondary)', marginTop: '2px' }}>{p.offerLabel}</div>}
                  </td>
                  <td>
                    <div style={{ maxWidth: '180px', fontSize: '13px' }}>{p.description || '—'}</div>
                  </td>
                  <td>
                    <strong style={{ color: 'var(--color-success)' }}>
                      {p.discountType === 'PERCENTAGE' ? `${p.discountValue}%` : `₹${p.discountValue}`}
                    </strong>
                    {p.maxDiscount && <div style={{ fontSize: '11px', color: 'var(--color-text-dim)' }}>max ₹{p.maxDiscount}</div>}
                  </td>
                  <td>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                      {getConditionBadges(p).length > 0 ? getConditionBadges(p) : <span style={{ color: 'var(--color-text-dim)', fontSize: '12px' }}>All customers</span>}
                    </div>
                    {p.startsAt && <div style={{ fontSize: '10px', color: 'var(--color-text-dim)', marginTop: '4px' }}><Clock size={10} /> {new Date(p.startsAt).toLocaleDateString()} — {p.expiresAt ? new Date(p.expiresAt).toLocaleDateString() : '∞'}</div>}
                  </td>
                  <td>
                    <strong>{p.usedCount}</strong>
                    {p.maxUses && <span style={{ color: 'var(--color-text-dim)' }}> / {p.maxUses}</span>}
                  </td>
                  <td>
                    {isExpired(p) ? <span className="badge danger">Expired</span> :
                     isExhausted(p) ? <span className="badge warning">Exhausted</span> :
                     p.isActive ? <span className="badge success">Active</span> :
                     <span className="badge">Inactive</span>}
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                      <button onClick={() => toggleActive(p)}
                        style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: p.isActive ? 'var(--color-success)' : 'var(--color-text-dim)' }}
                        title={p.isActive ? 'Deactivate' : 'Activate'}>
                        {p.isActive ? <ToggleRight size={20} /> : <ToggleLeft size={20} />}
                      </button>
                      <button onClick={() => openEdit(p)}
                        style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--color-secondary)' }}
                        title="Edit">
                        <Edit size={16} />
                      </button>
                      <button onClick={() => handleDelete(p.id)}
                        style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--color-danger)' }}
                        title="Delete">
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {promos.length === 0 && (
                <tr><td colSpan="7" style={{ textAlign: 'center', padding: '2rem' }}>
                  <Tag size={40} style={{ opacity: 0.3, marginBottom: '1rem' }} /><br/>
                  No promotions yet. Click "New Promo" to create one!
                </td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
