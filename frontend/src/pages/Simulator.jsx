import { useEffect, useState, useRef } from 'react';
import api from '../api';
import { User, MessageCircle, Send, Plus, Smartphone, History, MapPin } from 'lucide-react';

export default function Simulator() {
  const [messages, setMessages] = useState([]);
  const [phone, setPhone] = useState('1234567890');
  const [inputText, setInputText] = useState('');
  const [recentCustomers, setRecentCustomers] = useState([]);
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Load recent customers from dashboard to pick from
  useEffect(() => {
    const fetchCustomers = async () => {
      try {
        const res = await api.get('/dashboard/customers?size=10');
        setRecentCustomers(res.data.content);
      } catch (e) { console.error('Failed to fetch customers', e); }
    };
    fetchCustomers();
  }, []);

  // SSE Setup to listen for AI replies
  useEffect(() => {
    let eventSource;
    const token = localStorage.getItem('token');
    if (token) {
      eventSource = new EventSource(`/api/dashboard/sse/events?token=${token}`);
      eventSource.addEventListener('message', (event) => {
        try {
          const data = JSON.parse(event.data);
          if (data.type === 'message' && data.phone === phone) {
            // Check if user message is already in list (optional, but since we add manually after API call, 
            // the SSE will actually provide both confirmed)
            setMessages(prev => [
              ...prev,
              { id: Date.now() + '-user', direction: 'IN', content: data.customer, timestamp: new Date() },
              { id: Date.now() + '-bot', direction: 'OUT', content: data.bot, timestamp: new Date() }
            ]);
          }
        } catch (e) { console.error('SSE Error', e); }
      });
    }
    return () => eventSource && eventSource.close();
  }, [phone]);

  const handleSend = async (e) => {
    if (e) e.preventDefault();
    if (!inputText.trim() || loading) return;

    const text = inputText.trim();
    setInputText('');
    setLoading(true);

    try {
      await api.post('/simulator/send', {
        phone: phone,
        text: text,
        type: 'text'
      });
      // We don't add to messages here because SSE will push both back once processed
    } catch (e) {
      console.error('Send failed', e);
      alert('Failed to simulate message');
    } finally {
      setLoading(false);
    }
  };

  const sendLocation = async () => {
    if (loading) return;
    setLoading(true);
    try {
      await api.post('/simulator/send', {
        phone: phone,
        text: "Sent a location",
        type: 'location',
        lat: "12.9716",
        lng: "77.5946"
      });
    } catch (e) {
      alert('Failed to simulate location');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 8rem)', gap: '1.5rem', marginTop: '1rem' }}>
      
      {/* Sidebar: Phone/Customer Config */}
      <div className="glass-panel" style={{ width: '300px', padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        <div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Smartphone size={20} className="text-primary" /> Simulator Setup
          </h3>
          <label className="glass-label">Customer Phone Number</label>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <input 
              type="text" 
              className="glass-input" 
              value={phone} 
              onChange={(e) => setMessages([]) || setPhone(e.target.value)} 
              placeholder="e.g. 919876543210"
            />
          </div>
          <p style={{ fontSize: '0.75rem', marginTop: '0.5rem', opacity: 0.6 }}>
            Switching phone number clears the local chat trace.
          </p>
        </div>

        <div style={{ flex: 1, overflowY: 'auto' }}>
          <h4 style={{ fontSize: '0.9rem', marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem', opacity: 0.8 }}>
            <History size={16} /> Recent Customers
          </h4>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {recentCustomers.map(c => (
              <button 
                key={c.phone}
                onClick={() => { setMessages([]); setPhone(c.phone); }}
                style={{ 
                  background: phone === c.phone ? 'var(--color-primary-glow)' : 'rgba(255,255,255,0.05)',
                  border: '1px solid var(--color-glass-border)',
                  padding: '0.75rem',
                  borderRadius: 'var(--radius-md)',
                  color: 'white',
                  textAlign: 'left',
                  cursor: 'pointer',
                  fontSize: '0.85rem',
                  transition: 'var(--transition)'
                }}
              >
                <div style={{ fontWeight: 600 }}>{c.name || 'Anonymous'}</div>
                <div style={{ fontSize: '0.75rem', opacity: 0.7 }}>{c.phone}</div>
              </button>
            ))}
            <button 
              onClick={() => { setMessages([]); setPhone(Math.floor(1000000000 + Math.random() * 9000000000).toString()); }}
              className="glass-button" 
              style={{ fontSize: '0.75rem', padding: '0.5rem' }}
            >
              <Plus size={14} /> New Random Phone
            </button>
          </div>
        </div>
      </div>

      {/* Main Chat Interface */}
      <div className="glass-panel" style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', position: 'relative' }}>
        
        {/* Header */}
        <div style={{ padding: '1rem 1.5rem', borderBottom: '1px solid var(--color-glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(0,0,0,0.2)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div style={{ width: '40px', height: '40px', background: 'var(--color-primary-glow)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <User size={20} />
            </div>
            <div>
              <div style={{ fontWeight: 600 }}>WhatsApp Simulation</div>
              <div style={{ fontSize: '0.75rem', color: '#22c55e', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                <span style={{ width: '6px', height: '6px', background: '#22c55e', borderRadius: '50%' }}></span> Pipeline Active
              </div>
            </div>
          </div>
          <button onClick={sendLocation} className="glass-button" style={{ padding: '0.5rem 1rem', fontSize: '0.75rem', background: 'rgba(255,255,255,0.05)' }}>
            <MapPin size={14} /> Send Location
          </button>
        </div>

        {/* Message Trace */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {messages.length === 0 && (
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', opacity: 0.3 }}>
              <MessageCircle size={64} style={{ marginBottom: '1rem' }} />
              <p>Type a message below to start the simulation</p>
            </div>
          )}
          
          {messages.map((m, i) => (
            <div 
              key={m.id || i}
              style={{ 
                alignSelf: m.direction === 'IN' ? 'flex-start' : 'flex-end',
                maxWidth: '80%',
                animation: 'slideUp 0.3s ease-out'
              }}
            >
              <div style={{ 
                padding: '0.75rem 1rem', 
                borderRadius: '12px',
                borderTopLeftRadius: m.direction === 'IN' ? '2px' : '12px',
                borderTopRightRadius: m.direction === 'OUT' ? '2px' : '12px',
                background: m.direction === 'IN' ? 'rgba(255,255,255,0.1)' : 'var(--color-primary)',
                color: 'white',
                boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
              }}>
                <div style={{ fontSize: '0.65rem', opacity: 0.6, marginBottom: '0.25rem', fontWeight: 600 }}>
                  {m.direction === 'IN' ? 'CUSTOMER' : 'SYSTEM AI'}
                </div>
                <div style={{ whiteSpace: 'pre-wrap', lineHeight: '1.4' }}>{m.content}</div>
                <div style={{ fontSize: '0.6rem', textAlign: 'right', opacity: 0.5, marginTop: '0.4rem' }}>
                  {new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </div>
              </div>
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>

        {/* Input Area */}
        <form onSubmit={handleSend} style={{ padding: '1.25rem', background: 'rgba(0,0,0,0.3)', borderTop: '1px solid var(--color-glass-border)', display: 'flex', gap: '1rem' }}>
          <input 
            type="text" 
            className="glass-input" 
            placeholder="Type message as customer..." 
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            disabled={loading}
            style={{ borderRadius: '30px', paddingLeft: '1.5rem' }}
          />
          <button 
            type="submit" 
            className="glass-button primary" 
            disabled={!inputText.trim() || loading}
            style={{ width: '48px', height: '48px', borderRadius: '50%', padding: 0 }}
          >
            <Send size={20} />
          </button>
        </form>
      </div>

      <style>{`
        @keyframes slideUp {
          from { transform: translateY(10px); opacity: 0; }
          to { transform: translateY(0); opacity: 1; }
        }
      `}</style>
    </div>
  );
}
