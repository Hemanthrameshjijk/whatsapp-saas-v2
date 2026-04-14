import { useEffect, useState } from 'react';
import api from '../api';
import { Save } from 'lucide-react';

export default function Settings() {
  const [settings, setSettings] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState('store');

  useEffect(() => {
    api.get('/dashboard/settings').then(res => {
      setSettings(res.data);
      setLoading(false);
    }).catch(console.error);
  }, []);

  const handleSubmit = async (e) => {
    if (e) e.preventDefault();
    setSaving(true);
    try {
      await api.put('/dashboard/settings', settings);
      alert('Settings saved successfully!');
    } catch (e) {
      console.error(e);
      alert('Failed to save settings.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div style={{ padding: '2rem' }}>Loading Settings...</div>;

  return (
    <div style={{ maxWidth: '900px' }}>
      <div style={{ marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ margin: 0 }}>Store Configuration</h1>
          <p style={{ color: 'var(--color-text-dim)', margin: '0.5rem 0 0 0' }}>Configure your AI behavior and business rules</p>
        </div>
        <button className="glass-button primary" onClick={handleSubmit} disabled={saving}>
          <Save size={18} /> {saving ? 'Saving...' : 'Save All Changes'}
        </button>
      </div>

      <div style={{ display: 'flex', gap: '8px', marginBottom: '2rem' }}>
        <button className={`glass-button ${activeTab === 'store' ? 'active' : ''}`} onClick={() => setActiveTab('store')}>Store Profile</button>
        <button className={`glass-button ${activeTab === 'ai' ? 'active' : ''}`} onClick={() => setActiveTab('ai')}>AI Intelligence</button>
        <button className={`glass-button ${activeTab === 'policies' ? 'active' : ''}`} onClick={() => setActiveTab('policies')}>Policies & FAQ</button>
      </div>
      
      <div className="glass-card" style={{ padding: '2rem' }}>
        {activeTab === 'store' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', animation: 'fadeIn 0.3s' }}>
            <h3 style={{ margin: 0, color: 'var(--color-secondary)' }}>Payment & Logistics</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
              <div>
                <label className="glass-label">UPI ID (for payments)</label>
                <input className="glass-input" value={settings.upiId || ''} onChange={e => setSettings({...settings, upiId: e.target.value})} placeholder="yourname@upi" />
              </div>
              <div>
                <label className="glass-label">Shop Address</label>
                <input className="glass-input" value={settings.shopAddress || ''} onChange={e => setSettings({...settings, shopAddress: e.target.value})} />
              </div>
              <div>
                <label className="glass-label">Delivery Charge (₹)</label>
                <input className="glass-input" type="number" step="0.01" value={settings.deliveryCharge || 0} onChange={e => setSettings({...settings, deliveryCharge: e.target.value})} />
              </div>
              <div>
                <label className="glass-label">Free Delivery Above (₹)</label>
                <input className="glass-input" type="number" step="0.01" value={settings.freeDeliveryAbove || 0} onChange={e => setSettings({...settings, freeDeliveryAbove: e.target.value})} />
              </div>
            </div>
          </div>
        )}

        {activeTab === 'ai' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', animation: 'fadeIn 0.3s' }}>
            <h3 style={{ margin: 0, color: 'var(--color-primary)' }}>AI Personality & Safety</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
              <div style={{ gridColumn: 'span 2' }}>
                <label className="glass-label">Greeting Template</label>
                <input className="glass-input" value={settings.greetingTemplate || ''} onChange={e => setSettings({...settings, greetingTemplate: e.target.value})} placeholder="Hi {name}, how can I help you today?" />
              </div>
              <div>
                <label className="glass-label">Tone Style</label>
                <select className="glass-input" value={settings.toneStyle || 'Friendly'} onChange={e => setSettings({...settings, toneStyle: e.target.value})}>
                  <option value="Friendly">Friendly & Enthusiastic</option>
                  <option value="Professional">Formal & Professional</option>
                  <option value="Minimalist">Short & Direct</option>
                </select>
              </div>
              <div>
                <label className="glass-label">Primary Language</label>
                <input className="glass-input" value={settings.languageStyle || ''} onChange={e => setSettings({...settings, languageStyle: e.target.value})} placeholder="English / Tanglish" />
              </div>
              <div style={{ gridColumn: 'span 2' }}>
                <label className="glass-label">Competitor Keywords (Guardrails)</label>
                <textarea className="glass-input" style={{ minHeight: '80px' }} value={settings.competitorKeywords || ''} onChange={e => setSettings({...settings, competitorKeywords: e.target.value})} placeholder="Amazon, Flipkart, Local Market (comma separated)" />
              </div>
            </div>
          </div>
        )}

        {activeTab === 'policies' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', animation: 'fadeIn 0.3s' }}>
            <h3 style={{ margin: 0, color: '#ffb86c' }}>Global Store Policies</h3>
            
            <div style={{ display: 'grid', gap: '1.5rem' }}>
              <div className="glass-panel" style={{ padding: '1.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                  <h4 style={{ margin: 0 }}>Return Settings</h4>
                  <div style={{ display: 'flex', gap: '4px', background: 'rgba(0,0,0,0.2)', padding: '4px', borderRadius: '8px' }}>
                    {['ENABLED', 'DISABLED'].map(m => (
                      <button key={m} type="button" onClick={() => setSettings({...settings, globalReturnMode: m})} style={{ padding: '6px 12px', border: 'none', borderRadius: '6px', background: settings.globalReturnMode === m ? 'var(--color-secondary)' : 'transparent', color: '#fff', cursor: 'pointer', transition: '0.2s' }}>{m}</button>
                    ))}
                  </div>
                </div>
                {settings.globalReturnMode === 'ENABLED' && (
                  <textarea className="glass-input" value={settings.globalReturnPolicy || ''} onChange={e => setSettings({...settings, globalReturnPolicy: e.target.value})} placeholder="Describe your return policy..." />
                )}
              </div>

              <div className="glass-panel" style={{ padding: '1.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                  <h4 style={{ margin: 0 }}>Warranty Settings</h4>
                  <div style={{ display: 'flex', gap: '4px', background: 'rgba(0,0,0,0.2)', padding: '4px', borderRadius: '8px' }}>
                    {['ENABLED', 'DISABLED'].map(m => (
                      <button key={m} type="button" onClick={() => setSettings({...settings, globalWarrantyMode: m})} style={{ padding: '6px 12px', border: 'none', borderRadius: '6px', background: settings.globalWarrantyMode === m ? 'var(--color-primary)' : 'transparent', color: '#fff', cursor: 'pointer', transition: '0.2s' }}>{m}</button>
                    ))}
                  </div>
                </div>
                {settings.globalWarrantyMode === 'ENABLED' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <input className="glass-input" value={settings.globalWarrantyDetails || ''} onChange={e => setSettings({...settings, globalWarrantyDetails: e.target.value})} placeholder="Default Warranty Duration" />
                    <textarea className="glass-input" value={settings.globalWarrantyClaimRules || ''} onChange={e => setSettings({...settings, globalWarrantyClaimRules: e.target.value})} placeholder="General Claim Rules" />
                  </div>
                )}
              </div>

              <div>
                <label className="glass-label">Store FAQ & Knowledge Base</label>
                <textarea className="glass-input" rows="6" value={settings.generalFaq || ''} onChange={e => setSettings({...settings, generalFaq: e.target.value})} placeholder="Provide extra context for the AI agent about your shop..." />
              </div>
            </div>
          </div>
        )}
      </div>

      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(10px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  );
}
