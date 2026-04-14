import { useEffect, useState } from 'react';
import api from '../api';
import { Plus, Edit, Trash2 } from 'lucide-react';

export default function Products() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({ 
    name: '', price: '', category: '', sku: '', brand: '', type: '', color: '', size: '', stockQty: 10, description: '', outOfStock: false,
    warrantyMode: 'GLOBAL', warrantyDetails: '', warrantyClaimRules: '', 
    returnMode: 'GLOBAL', customReturnPolicy: ''
  });
  const [editingId, setEditingId] = useState(null);

  const toggleOutOfStock = async (p) => {
    try {
      await api.put(`/dashboard/products/${p.id}`, { outOfStock: !p.outOfStock });
      fetchProducts();
    } catch (e) { console.error(e); }
  };

  const fetchProducts = async () => {
    try {
      const res = await api.get('/dashboard/products?size=100');
      setProducts(res.data.content);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchProducts(); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        await api.put(`/dashboard/products/${editingId}`, formData);
      } else {
        await api.post('/dashboard/products', formData);
      }
      setShowModal(false);
      fetchProducts();
    } catch (e) { console.error(e); }
  };

  const handleDelete = async (id) => {
    if (confirm('Are you sure you want to delete this product?')) {
      await api.delete(`/dashboard/products/${id}`);
      fetchProducts();
    }
  };

  const openEdit = (p) => {
    setFormData({ 
      name: p.name, price: p.price, category: p.category, sku: p.sku || '', brand: p.brand || '', type: p.type || '', color: p.color || '', size: p.size || '', stockQty: p.stockQty, description: p.description, outOfStock: p.outOfStock || false,
      warrantyMode: p.warrantyMode || 'GLOBAL', warrantyDetails: p.warrantyDetails || '', warrantyClaimRules: p.warrantyClaimRules || '', 
      returnMode: p.returnMode || 'GLOBAL', customReturnPolicy: p.customReturnPolicy || ''
    });
    setEditingId(p.id);
    setShowModal(true);
  };

  const openCreate = () => {
    setFormData({ 
      name: '', price: '', category: '', sku: '', brand: '', type: '', color: '', size: '', stockQty: 10, description: '', outOfStock: false,
      warrantyMode: 'GLOBAL', warrantyDetails: '', warrantyClaimRules: '', 
      returnMode: 'GLOBAL', customReturnPolicy: ''
    });
    setEditingId(null);
    setShowModal(true);
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h1 style={{ margin: 0 }}>Products</h1>
        <button className="glass-button" onClick={openCreate}><Plus size={18} /> New Product</button>
      </div>

      <div className="glass-table-container">
        {loading ? <p style={{ padding: '1rem' }}>Loading...</p> : (
          <table className="glass-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>SKU</th>
                <th>Brand</th>
                <th>Category</th>
                <th>Type</th>
                <th>Color</th>
                <th>Size</th>
                <th>Price</th>
                <th>Stock</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {products.map(p => (
                <tr key={p.id}>
                  <td>
                    <strong>{p.name}</strong><br/>
                    <small style={{ color: 'var(--color-text-dim)' }}>{p.description}</small>
                  </td>
                  <td>{p.sku || '-'}</td>
                  <td>{p.brand || '-'}</td>
                  <td><span className="badge info">{p.category || '-'}</span></td>
                  <td>{p.type || '-'}</td>
                  <td>{p.color || '-'}</td>
                  <td>{p.size || '-'}</td>
                  <td>₹{p.price}</td>
                  <td>
                    {p.outOfStock ? (
                      <span className="badge danger">Out of Stock</span>
                    ) : (
                      <span className={`badge ${p.stockQty > 5 ? 'success' : 'danger'}`}>{p.stockQty}</span>
                    )}
                  </td>
                  <td>
                    <button style={{ background: 'var(--color-primary)', border: 'none', color: '#fff', cursor: 'pointer', marginRight: '1rem', padding: '4px 8px', borderRadius: '4px', fontSize: '12px' }} onClick={() => toggleOutOfStock(p)}>
                      {p.outOfStock ? 'Mark In Stock' : 'Mark Out of Stock'}
                    </button>
                    <button style={{ background: 'transparent', border: 'none', color: 'var(--color-secondary)', cursor: 'pointer', marginRight: '0.5rem' }} onClick={() => openEdit(p)}>
                      <Edit size={18} />
                    </button>
                    <button style={{ background: 'transparent', border: 'none', color: 'var(--color-accent)', cursor: 'pointer' }} onClick={() => handleDelete(p.id)}>
                      <Trash2 size={18} />
                    </button>
                  </td>
                </tr>
              ))}
              {products.length === 0 && (
                <tr><td colSpan="10" style={{ textAlign: 'center' }}>No products found. Add one to get started!</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>

      {showModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh', background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 }}>
          <div className="glass-card" style={{ width: '500px', maxWidth: '90%' }}>
            <h2>{editingId ? 'Edit Product' : 'New Product'}</h2>
            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '1.5rem' }}>
              <input className="glass-input" placeholder="Product Name" required value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} />
              <div style={{ display: 'flex', gap: '1rem' }}>
                <input className="glass-input" placeholder="SKU" value={formData.sku} onChange={e => setFormData({...formData, sku: e.target.value})} />
                <input className="glass-input" placeholder="Brand" value={formData.brand} onChange={e => setFormData({...formData, brand: e.target.value})} />
              </div>
              <div style={{ display: 'flex', gap: '1rem' }}>
                <input className="glass-input" placeholder="Category" value={formData.category} onChange={e => setFormData({...formData, category: e.target.value})} />
                <input className="glass-input" placeholder="Type" value={formData.type} onChange={e => setFormData({...formData, type: e.target.value})} />
              </div>
              <div style={{ display: 'flex', gap: '1rem' }}>
                <input className="glass-input" placeholder="Color" value={formData.color} onChange={e => setFormData({...formData, color: e.target.value})} />
                <input className="glass-input" placeholder="Size" value={formData.size} onChange={e => setFormData({...formData, size: e.target.value})} />
              </div>
              <div style={{ display: 'flex', gap: '1rem' }}>
                <input className="glass-input" type="number" step="0.01" placeholder="Price (₹)" required value={formData.price} onChange={e => setFormData({...formData, price: e.target.value})} />
                <input className="glass-input" type="number" placeholder="Stock Qty" required value={formData.stockQty} onChange={e => setFormData({...formData, stockQty: e.target.value})} />
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <input type="checkbox" id="outOfStock" checked={formData.outOfStock} onChange={e => setFormData({...formData, outOfStock: e.target.checked})} />
                <label htmlFor="outOfStock" style={{ color: 'var(--color-text)' }}>Mark as Out of Stock</label>
              </div>
              <textarea className="glass-input" placeholder="Description" rows="2" value={formData.description} onChange={e => setFormData({...formData, description: e.target.value})} />
              
              <div style={{ height: '1px', background: 'var(--color-glass-border)', margin: '0.5rem 0' }}></div>
              
              {/* Warranty Section */}
              <div style={{ padding: '0.5rem', border: '1px solid var(--color-glass-border)', borderRadius: '8px' }}>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>Warranty Selection</label>
                <div style={{ display: 'flex', gap: '1rem', mb: '0.5rem' }}>
                  {['GLOBAL', 'CUSTOM', 'NONE'].map(mode => (
                    <label key={mode} style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', cursor: 'pointer', fontSize: '13px' }}>
                      <input type="radio" name="warrantyMode" value={mode} checked={formData.warrantyMode === mode} onChange={e => setFormData({...formData, warrantyMode: e.target.value})} />
                      {mode}
                    </label>
                  ))}
                </div>
                {formData.warrantyMode === 'CUSTOM' && (
                  <div style={{ marginTop: '0.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    <input className="glass-input" placeholder="Warranty Duration (e.g. 6 Months)" value={formData.warrantyDetails} onChange={e => setFormData({...formData, warrantyDetails: e.target.value})} />
                    <textarea className="glass-input" placeholder="Claim Rules" rows="1" value={formData.warrantyClaimRules} onChange={e => setFormData({...formData, warrantyClaimRules: e.target.value})} />
                  </div>
                )}
                <p style={{ fontSize: '11px', color: 'var(--color-text-dim)', marginTop: '0.25rem' }}>
                  {formData.warrantyMode === 'GLOBAL' && 'Uses store-wide default warranty from AI Settings.'}
                  {formData.warrantyMode === 'CUSTOM' && 'Set unique warranty details for this item.'}
                  {formData.warrantyMode === 'NONE' && 'Strictly NO warranty for this specific item.'}
                </p>
              </div>

              {/* Returns Section */}
              <div style={{ padding: '0.5rem', border: '1px solid var(--color-glass-border)', borderRadius: '8px' }}>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>Return Policy</label>
                <div style={{ display: 'flex', gap: '1rem', mb: '0.5rem' }}>
                  {['GLOBAL', 'CUSTOM', 'NONE'].map(mode => (
                    <label key={mode} style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', cursor: 'pointer', fontSize: '13px' }}>
                      <input type="radio" name="returnMode" value={mode} checked={formData.returnMode === mode} onChange={e => setFormData({...formData, returnMode: e.target.value})} />
                      {mode}
                    </label>
                  ))}
                </div>
                {formData.returnMode === 'CUSTOM' && (
                  <div style={{ marginTop: '0.5rem' }}>
                    <textarea className="glass-input" placeholder="Specific Return Instructions" rows="1" value={formData.customReturnPolicy} onChange={e => setFormData({...formData, customReturnPolicy: e.target.value})} />
                  </div>
                )}
                <p style={{ fontSize: '11px', color: 'var(--color-text-dim)', marginTop: '0.25rem' }}>
                  {formData.returnMode === 'GLOBAL' && 'Uses store-wide default return policy.'}
                  {formData.returnMode === 'CUSTOM' && 'Override global policy with these specific rules.'}
                  {formData.returnMode === 'NONE' && 'Item is NON-RETURNABLE (Final sale).'}
                </p>
              </div>
              
              <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
                <button type="button" className="glass-button" style={{ background: 'rgba(255,255,255,0.1)', flex: 1 }} onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="glass-button" style={{ flex: 1 }}>Save</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
