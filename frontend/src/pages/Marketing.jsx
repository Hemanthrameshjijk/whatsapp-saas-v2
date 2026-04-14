import { useEffect, useState } from 'react';
import api from '../api';
import { Megaphone, ShoppingBag, Send, Users, Clock, AlertCircle, CheckCircle, Info, History, Link, Plus, Copy, ExternalLink, MessageCircle, BarChart3, Zap } from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';

export default function Marketing() {
  const [activeTab, setActiveTab] = useState('abandoned');
  
  const fetchAnalytics = async () => {
    setLoading(true);
    // V23 Baseline
    setLoading(false);
  };

  const fetchHistory = async () => {
    setLoading(true);
    try {
      const res = await api.get('/dashboard/marketing/history');
      setCampaigns(res.data);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  };
  const [abandonedCarts, setAbandonedCarts] = useState([]);
  const [campaigns, setCampaigns] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // Broadcast state
  const [broadcastMsg, setBroadcastMsg] = useState('');
  const [audience, setAudience] = useState('ALL');
  const [promoId, setPromoId] = useState('');
  const [promos, setPromos] = useState([]);
  const [sending, setSending] = useState(false);
  const [lastStats, setLastStats] = useState(null);

  // Influencer state
  const [influencerLinks, setInfluencerLinks] = useState([]);
  const [products, setProducts] = useState([]);
  const [newLink, setNewLink] = useState({ name: '', productId: '', targetUrl: '', customCode: '', type: 'whatsapp' });
  const [creating, setCreating] = useState(false);
  const [activities, setActivities] = useState([]);

  const fetchAbandoned = async () => {
    setLoading(true);
    try {
      const res = await api.get('/dashboard/marketing/abandoned-carts');
      setAbandonedCarts(res.data);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  };

  const fetchPromos = async () => {
    try {
      const res = await api.get('/dashboard/promotions?size=100');
      setPromos(res.data.content || []);
    } catch (e) { console.error(e); }
  };

  const fetchInfluencerLinks = async () => {
    setLoading(true);
    try {
      const res = await api.get('/dashboard/marketing/links/influencers');
      setInfluencerLinks(res.data);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  };

  const fetchProducts = async () => {
    try {
      const res = await api.get('/dashboard/products?size=100');
      setProducts(res.data.content || []);
    } catch (e) { console.error(e); }
  };


  useEffect(() => {
    let eventSource;
    const token = localStorage.getItem('token');
    if (token) {
      eventSource = new EventSource(`/api/dashboard/sse/events?token=${token}`);
      eventSource.addEventListener('marketing', (event) => {
        try {
          const data = JSON.parse(event.data);
          setActivities(prev => [{
            id: Date.now(),
            icon: data.icon,
            title: data.title,
            description: data.description,
            time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
          }, ...prev].slice(0, 10)); // Keep last 10
        } catch (e) { console.error(e); }
      });
    }
    return () => eventSource && eventSource.close();
  }, []);

  useEffect(() => {
    if (activeTab === 'abandoned') fetchAbandoned();
    if (activeTab === 'broadcast') fetchPromos();
    if (activeTab === 'history') fetchHistory();
    if (activeTab === 'influencers') {
      fetchInfluencerLinks();
      fetchProducts();
    }
    if (activeTab === 'analytics') {
      fetchAnalytics();
    }
  }, [activeTab]);

  const sendReminder = async (phone, nudgePromoId) => {
    try {
      await api.post('/dashboard/marketing/remind', { phone, promoId: nudgePromoId });
      alert('Reminder with offer sent successfully!');
    } catch (e) { alert('Failed to send reminder.'); }
  };

  const createManualLink = async () => {
    if (!newLink.name) return alert('Please enter influencer/campaign name');
    setCreating(true);
    try {
      await api.post('/dashboard/marketing/links/manual', newLink);
      setNewLink({ name: '', productId: '', targetUrl: '', customCode: '', type: 'whatsapp' });
      fetchInfluencerLinks();
      alert('Tracking link created successfully!');
    } catch (e) { alert('Failed to create link.'); }
    finally { setCreating(false); }
  };

  const copyToClipboard = (code) => {
    const url = `https://ai.whatsappstore.com/l/${code}`;
    navigator.clipboard.writeText(url);
    alert('Link copied to clipboard!');
  };

  return (
    <div>
      <div style={{ marginBottom: '2rem' }}>
        <h1>Growth & Marketing</h1>
        <p style={{ color: 'var(--color-text-dim)' }}>Recover lost sales and run promotional campaigns</p>
      </div>

      <div style={{ display: 'flex', gap: '8px', marginBottom: '2rem' }}>
        <button 
          className={`glass-button ${activeTab === 'abandoned' ? 'active' : ''}`}
          onClick={() => setActiveTab('abandoned')}
          style={{ display: 'flex', gap: '8px', alignItems: 'center' }}
        >
          <ShoppingBag size={18} /> Abandoned Carts
        </button>
        <button 
          className={`glass-button ${activeTab === 'broadcast' ? 'active' : ''}`}
          onClick={() => setActiveTab('broadcast')}
          style={{ display: 'flex', gap: '8px', alignItems: 'center' }}
        >
          <Megaphone size={18} /> Promotional Broadcast
        </button>
        <button 
          className={`glass-button ${activeTab === 'history' ? 'active' : ''}`}
          onClick={() => setActiveTab('history')}
          style={{ display: 'flex', gap: '8px', alignItems: 'center' }}
        >
          <History size={18} /> Campaign History
        </button>
        <button 
          className={`glass-button ${activeTab === 'influencers' ? 'active' : ''}`}
          onClick={() => setActiveTab('influencers')}
          style={{ display: 'flex', gap: '8px', alignItems: 'center' }}
        >
          <Users size={18} /> Influencer Manager
        </button>
        <button 
          className={`glass-button ${activeTab === 'analytics' ? 'active' : ''}`}
          onClick={() => setActiveTab('analytics')}
          style={{ display: 'flex', gap: '8px', alignItems: 'center' }}
        >
          <BarChart3 size={18} /> Analytics & ROI
        </button>
      </div>

      <div style={{ display: 'flex', gap: '2rem', alignItems: 'flex-start' }}>
        <div style={{ flex: 1 }}>
          {activeTab === 'abandoned' && (
        <div className="glass-table-container">
          <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--color-glass-border)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Clock size={18} style={{ color: 'var(--color-warning)' }} />
            <h3 style={{ margin: 0 }}>High-Intent Shoppers</h3>
          </div>
          {loading ? <p style={{ padding: '2rem' }}>Scanning for active carts...</p> : (
            <table className="glass-table">
              <thead>
                <tr>
                  <th>Customer</th>
                  <th>Cart Items</th>
                  <th>Value</th>
                  <th>Smart Nudge</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {abandonedCarts.map(cart => (
                  <tr key={cart.phone}>
                    <td>
                      <strong>{cart.name}</strong><br/>
                      <small style={{ color: 'var(--color-text-dim)' }}>{cart.phone}</small>
                    </td>
                    <td>
                      <div style={{ fontSize: '13px' }}>
                        {cart.items.map((item, idx) => (
                          <div key={idx}>{item.productName} x{item.quantity}</div>
                        ))}
                      </div>
                    </td>
                    <td><strong style={{ color: 'var(--color-secondary)' }}>₹{cart.total}</strong></td>
                    <td>
                      {cart.suggestedNudge ? (
                        <span className="badge success" style={{ fontSize: '11px' }}>
                          ✨ Suggesting {cart.suggestedNudge}
                        </span>
                      ) : (
                        <span style={{ color: 'var(--color-text-dim)', fontSize: '11px' }}>No valid offer</span>
                      )}
                    </td>
                    <td>
                      <button className="glass-button" style={{ fontSize: '12px', padding: '0.4rem 0.8rem' }} onClick={() => sendReminder(cart.phone, cart.nudgePromoId)}>
                        <Send size={14} /> Send with Offer
                      </button>
                    </td>
                  </tr>
                ))}
                {abandonedCarts.length === 0 && (
                  <tr><td colSpan="5" style={{ textAlign: 'center', padding: '3rem' }}>
                    <AlertCircle size={40} style={{ opacity: 0.2, marginBottom: '1rem' }} /><br/>
                    No active abandoned carts found.
                  </td></tr>
                )}
              </tbody>
            </table>
          )}
        </div>
      )}

      {activeTab === 'history' && (
        <div className="glass-table-container">
          <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--color-glass-border)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <History size={18} style={{ color: 'var(--color-secondary)' }} />
            <h3 style={{ margin: 0 }}>Broadcast History</h3>
          </div>
          {loading ? <p style={{ padding: '2rem' }}>Loading campaign logs...</p> : (
            <table className="glass-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Segment</th>
                  <th>Message Preview</th>
                  <th>Sent</th>
                  <th>Clicks / CTR</th>
                </tr>
              </thead>
              <tbody>
                {campaigns.map(camp => {
                  const ctr = camp.sentCount > 0 ? ((camp.clickCount / camp.sentCount) * 100).toFixed(1) : 0;
                  return (
                    <tr key={camp.id}>
                      <td>
                        <strong>{new Date(camp.createdAt).toLocaleDateString()}</strong><br/>
                        <small style={{ color: 'var(--color-text-dim)' }}>{new Date(camp.createdAt).toLocaleTimeString()}</small>
                      </td>
                      <td><span className="badge info">{camp.audience}</span></td>
                      <td style={{ maxWidth: '300px' }}>
                        <div style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', fontSize: '13px' }}>
                          {camp.message}
                        </div>
                      </td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <CheckCircle size={14} style={{ color: 'var(--color-success)' }} />
                          <span>{camp.sentCount}</span>
                        </div>
                      </td>
                      <td>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <strong>{camp.clickCount}</strong>
                            <small style={{ color: 'var(--color-text-dim)' }}>clicks</small>
                          </div>
                          <span className={`badge ${ctr > 10 ? 'success' : ctr > 5 ? 'warning' : 'info'}`} style={{ fontSize: '10px', padding: '2px 6px' }}>
                            {ctr}% CTR
                          </span>
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {campaigns.length === 0 && (
                  <tr><td colSpan="4" style={{ textAlign: 'center', padding: '3rem' }}>No campaigns found.</td></tr>
                )}
              </tbody>
            </table>
          )}
        </div>
      )}

      {activeTab === 'broadcast' && (
        <div className="glass-card" style={{ maxWidth: '800px' }}>
          <div style={{ marginBottom: '2rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div style={{ padding: '1rem', background: 'var(--color-primary-glow)', borderRadius: '12px' }}>
              <Users size={32} />
            </div>
            <div>
              <h2 style={{ margin: 0 }}>Launch Broadcast</h2>
              <p style={{ margin: 0, opacity: 0.7 }}>Send personalized messages to your customers</p>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '1.5rem' }}>
            <div>
              <label className="glass-label">Audience Segment</label>
              <select className="glass-input" value={audience} onChange={e => setAudience(e.target.value)}>
                <option value="ALL">All Customers (Reach everyone)</option>
                <option value="VIPS">High Value Customers (LTV {'>'} ₹1000)</option>
              </select>
            </div>
            <div>
              <label className="glass-label">Attach Promotion (Optional)</label>
              <select className="glass-input" value={promoId} onChange={e => setPromoId(e.target.value)}>
                <option value="">No offer attached</option>
                {promos.map(p => (
                  <option key={p.id} value={p.id}>{p.offerLabel || p.code} ({p.discountValue}{p.discountType === 'PERCENTAGE' ? '%' : '₹'} OFF)</option>
                ))}
              </select>
            </div>
          </div>

          <div style={{ marginBottom: '1.5rem' }}>
            <label className="glass-label">Your Message</label>
            <textarea 
              className="glass-input" 
              style={{ minHeight: '150px', resize: 'vertical' }}
              placeholder="Hi {name}, we have a special sale for you!..."
              value={broadcastMsg}
              onChange={e => setBroadcastMsg(e.target.value)}
            />
            <small style={{ color: 'var(--color-text-dim)', marginTop: '8px', display: 'block' }}>
              <Info size={12} style={{ verticalAlign: 'middle', marginRight: '4px' }} /> 
              Use <strong>{"{name}"}</strong> to personalize the message.
            </small>
          </div>

          <div className="glass-panel" style={{ padding: '1rem', marginBottom: '1.5rem', backgroundColor: 'rgba(0,0,0,0.1)' }}>
            <h4 style={{ marginBottom: '0.5rem' }}>Preview:</h4>
            <p style={{ margin: 0, fontSize: '14px', whiteSpace: 'pre-wrap' }}>
              {broadcastMsg.replace(/{name}/g, 'Customer Name') || 'Type a message to see preview...'}
              {promoId && promos.find(p => p.id === promoId) && (
                <span style={{ color: 'var(--color-secondary)', fontWeight: 600 }}>
                  {"\n\n"}Use code *{promos.find(p => p.id === promoId).code}* to get {promos.find(p => p.id === promoId).discountValue}{promos.find(p => p.id === promoId).discountType === 'PERCENTAGE' ? '%' : '₹'} OFF!
                </span>
              )}
            </p>
          </div>

          <button 
            className="glass-button primary" 
            style={{ width: '100%', height: '50px', fontSize: '1.1rem' }} 
            onClick={sendBroadcast}
            disabled={sending}
          >
            {sending ? 'Sending Broadcast...' : <><Send size={20} /> Launch Campaign</>}
          </button>

          {lastStats && (
            <div style={{ marginTop: '1.5rem', padding: '1rem', borderRadius: '8px', backgroundColor: 'rgba(34, 197, 94, 0.1)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <CheckCircle size={18} style={{ color: 'var(--color-success)' }} />
              <span>Broadcast sent to {lastStats.sent} customers in the "{lastStats.audience}" segment!</span>
            </div>
          )}
        </div>
      )}

      {activeTab === 'influencers' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem', animation: 'fadeIn 0.3s' }}>
          <div className="glass-card" style={{ maxWidth: '800px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
              <div style={{ padding: '0.8rem', background: 'var(--color-primary-glow)', borderRadius: '10px' }}>
                <Plus size={24} />
              </div>
              <div>
                <h3 style={{ margin: 0 }}>Create Tracking Link</h3>
                <p style={{ margin: 0, fontSize: '14px', color: 'var(--color-text-dim)' }}>Generate influencer or bio links</p>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '8px', marginBottom: '1.5rem' }}>
              <button className={`glass-button ${newLink.type === 'whatsapp' ? 'active' : ''}`} onClick={() => setNewLink({...newLink, type: 'whatsapp'})}>
                <MessageCircle size={16} /> WhatsApp Deep-Link
              </button>
              <button className={`glass-button ${newLink.type === 'website' ? 'active' : ''}`} onClick={() => setNewLink({...newLink, type: 'website'})}>
                <ExternalLink size={16} /> Website Redirect
              </button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
              <div style={{ gridColumn: 'span 2' }}>
                <label className="glass-label">Influencer / Campaign Name</label>
                <input className="glass-input" placeholder="e.g. Rahul-Instagram-Stories" value={newLink.name} onChange={e => setNewLink({...newLink, name: e.target.value})} />
              </div>

              {newLink.type === 'whatsapp' ? (
                <div style={{ gridColumn: 'span 2' }}>
                  <label className="glass-label">Select Target Product</label>
                  <select className="glass-input" value={newLink.productId} onChange={e => setNewLink({...newLink, productId: e.target.value})}>
                    <option value="">-- No specific product --</option>
                    {products.map(p => <option key={p.id} value={p.id}>{p.name} (₹{p.price})</option>)}
                  </select>
                </div>
              ) : (
                <div style={{ gridColumn: 'span 2' }}>
                  <label className="glass-label">Target Website URL</label>
                  <input className="glass-input" placeholder="https://yourstore.com/collection/summer" value={newLink.targetUrl} onChange={e => setNewLink({...newLink, targetUrl: e.target.value})} />
                </div>
              )}

              <div style={{ gridColumn: 'span 2' }}>
                <label className="glass-label">Custom Short Code (Optional)</label>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ color: 'var(--color-text-dim)' }}>ai.whatsappstore.com/l/</span>
                  <input className="glass-input" placeholder="RAHUL20" value={newLink.customCode} onChange={e => setNewLink({...newLink, customCode: e.target.value})} />
                </div>
              </div>
            </div>

            <button className="glass-button primary" style={{ marginTop: '2rem', width: '100%' }} onClick={createManualLink} disabled={creating}>
              {creating ? 'Creating...' : 'Generate & Save Link'}
            </button>
          </div>

          <div className="glass-table-container">
            <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--color-glass-border)' }}>
              <h3 style={{ margin: 0 }}>Active Tracking Links</h3>
            </div>
            <table className="glass-table">
              <thead>
                <tr>
                  <th>Influencer / Source</th>
                  <th>Type</th>
                  <th>Tracking URL</th>
                  <th>Clicks</th>
                  <th>Acquired</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {influencerLinks.map(link => (
                  <tr key={link.id}>
                    <td>
                      <strong>{link.influencerName}</strong><br/>
                      <small style={{ color: 'var(--color-text-dim)' }}>{new Date(link.createdAt).toLocaleDateString()}</small>
                    </td>
                    <td>
                      {link.productId ? (
                        <span className="badge info" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><MessageCircle size={10} /> WhatsApp</span>
                      ) : (
                        <span className="badge secondary" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><ExternalLink size={10} /> Web</span>
                      )}
                    </td>
                    <td>
                      <code style={{ fontSize: '11px', color: 'var(--color-secondary)' }}>/l/{link.shortCode}</code>
                    </td>
                    <td>
                      <strong style={{ fontSize: '16px' }}>{link.clickCount}</strong>
                    </td>
                    <td>
                      <div className="badge success" style={{ display: 'flex', alignItems: 'center', gap: '4px', padding: '4px 8px' }}>
                        <Users size={12} />
                        <strong>{link.acquiredCount || 0}</strong>
                      </div>
                    </td>
                    <td>
                      <button className="glass-button" onClick={() => copyToClipboard(link.shortCode)}>
                        <Copy size={14} /> Copy
                      </button>
                    </td>
                  </tr>
                ))}
                {influencerLinks.length === 0 && (
                  <tr><td colSpan="6" style={{ textAlign: 'center', padding: '3rem' }}>No influencer links created yet.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {activeTab === 'analytics' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem', animation: 'fadeIn 0.3s' }}>
          <div className="glass-card" style={{ maxWidth: '800px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
              <BarChart3 size={24} style={{ color: 'var(--color-primary)' }} />
              <h3 style={{ margin: 0 }}>Marketing Analytics</h3>
            </div>
            <p style={{ color: 'var(--color-text-dim)' }}>Campaign performance and attribution data will appear here.</p>
          </div>
        </div>
      )}
      </div>

      {/* Live Activity Sidebar */}
      {(activeTab === 'history' || activeTab === 'influencers') && (
        <div className="glass-card" style={{ width: '300px', flexShrink: 0, height: 'fit-content', position: 'sticky', top: '1rem' }}>
          <div style={{ borderBottom: '1px solid var(--color-glass-border)', paddingBottom: '1rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Zap size={18} style={{ color: 'var(--color-secondary)' }} />
            <h3 style={{ margin: 0, fontSize: '1rem' }}>Live Activity</h3>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {activities.length === 0 ? (
              <p style={{ opacity: 0.5, fontSize: '13px', textAlign: 'center', margin: '2rem 0' }}>Waiting for activity... 📡</p>
            ) : activities.map(act => (
              <div key={act.id} style={{ 
                padding: '0.75rem', borderRadius: '8px', 
                background: 'rgba(255,255,255,0.03)', borderLeft: '3px solid var(--color-primary)',
                animation: 'slideLeft 0.3s ease-out'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                  <span style={{ fontWeight: 600, fontSize: '13px' }}>{act.title}</span>
                  <span style={{ fontSize: '10px', opacity: 0.5 }}>{act.time}</span>
                </div>
                <div style={{ fontSize: '12px', opacity: 0.7 }}>{act.description}</div>
              </div>
            ))}
          </div>
        </div>
      )}
      </div>

      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(10px); }
          to { opacity: 1; transform: translateY(0); }
        }
        @keyframes slideLeft {
          from { opacity: 0; transform: translateX(20px); }
          to { opacity: 1; transform: translateX(0); }
        }
      `}</style>
    </div>
  );
}
