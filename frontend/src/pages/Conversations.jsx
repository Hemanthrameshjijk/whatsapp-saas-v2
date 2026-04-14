import { useEffect, useState, useRef } from 'react';
import api from '../api';
import { User, MessageCircle, Search, Send, Shield, ShieldOff, CheckCircle, ShieldCheck, ShieldAlert } from 'lucide-react';

export default function Conversations() {
  const [activeChats, setActiveChats] = useState([]);
  const [filteredChats, setFilteredChats] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [messages, setMessages] = useState([]);
  const [selectedPhone, setSelectedPhone] = useState(null);
  
  const [loadingContacts, setLoadingContacts] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const messagesEndRef = useRef(null);
  const [error, setError] = useState(null);
  const [replyText, setReplyText] = useState('');
  const [sendingReply, setSendingReply] = useState(false);

  const selectedPhoneRef = useRef(selectedPhone);
  useEffect(() => {
    selectedPhoneRef.current = selectedPhone;
  }, [selectedPhone]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Fetch recent active chats for sidebar
  useEffect(() => {
    const fetchRecentChats = async () => {
      try {
        const res = await api.get('/dashboard/conversations/recent');
        setActiveChats(res.data);
      } catch (e) {
        console.error(e);
        setError('Failed to load active chats.');
      } finally {
        setLoadingContacts(false);
      }
    };
    fetchRecentChats();
  }, []);

  // Filter chats locally based on search
  useEffect(() => {
    if (!searchQuery.trim()) {
      setFilteredChats(activeChats);
      return;
    }
    const q = searchQuery.toLowerCase();
    setFilteredChats(activeChats.filter(c => 
      (c.customerName && c.customerName.toLowerCase().includes(q)) || 
      (c.phone && c.phone.includes(q))
    ));
  }, [searchQuery, activeChats]);

  // Fetch messages when a chat is selected
  useEffect(() => {
    const fetchMessages = async () => {
      if (!selectedPhone) return;
      setLoadingMessages(true);
      setError(null);
      setMessages([]);
      try {
        // Backend returns descending (newest first). We reverse it for chat UI (oldest top, newest bottom).
        const url = `/dashboard/conversations?phone=${encodeURIComponent(selectedPhone)}&size=100`;
        const res = await api.get(url);
        setMessages([...res.data.content].reverse());
      } catch (e) {
        console.error(e);
        setError('Failed to load conversations.');
      } finally {
        setLoadingMessages(false);
      }
    };
    fetchMessages();
  }, [selectedPhone]);

  // SSE Setup
  useEffect(() => {
    let eventSource;
    const token = localStorage.getItem('token');
    if (token) {
      eventSource = new EventSource(`/api/dashboard/sse/events?token=${token}`);
      eventSource.addEventListener('message', (event) => {
        try {
          const newMsg = JSON.parse(event.data);
          
          // Render new arrival in active chat if matching
          if (selectedPhoneRef.current === newMsg.phone) {
              setMessages(prev => [...prev,
                { id: Date.now() + '-in', direction: 'IN',  content: newMsg.customer, customerPhone: newMsg.phone, createdAt: new Date().toISOString() },
                { id: Date.now() + '-out', direction: 'OUT', content: newMsg.bot,      customerPhone: newMsg.phone, createdAt: new Date().toISOString() }
              ]);
              setTimeout(scrollToBottom, 100);
          }

          // Bump conversation to top in sidebar with the new message preview
          setActiveChats(prev => {
             const existing = prev.find(c => c.phone === newMsg.phone);
             const list = prev.filter(c => c.phone !== newMsg.phone);
             
             const updatedChat = existing ? {
                 ...existing, 
                 message: newMsg.bot, // Show bot's reply as the outbox
                 direction: 'OUT', 
                 timestamp: new Date().toISOString()
             } : {
                 phone: newMsg.phone, 
                 customerName: 'New Contact', 
                 message: newMsg.bot, 
                 direction: 'OUT', 
                 timestamp: new Date().toISOString()
             };
             return [updatedChat, ...list];
          });

        } catch (e) { console.error('SSE parse error', e); }
      });
      eventSource.onerror = (e) => console.warn('SSE connection error', e);
    }
    return () => eventSource && eventSource.close();
  }, []);

  const formatTime = (isoString) => {
    if (!isoString) return '';
    const d = new Date(isoString);
    const isToday = new Date().toDateString() === d.toDateString();
    if (isToday) {
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    return d.toLocaleDateString([], { month: 'short', day: 'numeric' });
  };

  const handleMarkHandled = async () => {
    if (!selectedPhone) return;
    try {
      await api.patch(`/dashboard/customers/${selectedPhone}/handoff`, { requiresHuman: false });
      // Update local state to remove the status
      setActiveChats(prev => prev.map(c => 
        c.phone === selectedPhone ? { ...c, requiresHuman: false, requiresHumanReason: null } : c
      ));
    } catch (e) {
      console.error(e);
      alert('Failed to enable AI.');
    }
  };

  const handleTakeOver = async () => {
    if (!selectedPhone) return;
    try {
      await api.patch(`/dashboard/customers/${selectedPhone}/handoff`, { requiresHuman: true });
      setActiveChats(prev => prev.map(c => 
        c.phone === selectedPhone ? { ...c, requiresHuman: true } : c
      ));
    } catch (e) { console.error(e); }
  };

  const handleSendReply = async () => {
    if (!selectedPhone || !replyText.trim() || sendingReply) return;
    
    setSendingReply(true);
    const msgContent = replyText.trim();
    try {
      await api.post(`/dashboard/conversations/${selectedPhone}/reply`, { message: msgContent });
      
      // Update local messages instantly
      const newMsg = {
        id: Date.now(),
        direction: 'OUT',
        content: msgContent,
        createdAt: new Date().toISOString(),
        manual: true
      };
      setMessages(prev => [...prev, newMsg]);
      setReplyText('');
      
      // Update sidebar preview
      setActiveChats(prev => prev.map(c => 
        c.phone === selectedPhone ? { ...c, message: msgContent, direction: 'OUT', timestamp: new Date().toISOString(), requiresHuman: true } : c
      ));
      
      setTimeout(scrollToBottom, 50);
    } catch (e) {
      alert('Failed to send message.');
    } finally {
      setSendingReply(false);
    }
  };

  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 4.5rem)', gap: '0', marginTop: '0', background: 'var(--color-bg)', borderRadius: 'var(--radius-lg)', overflow: 'hidden', boxShadow: '0 10px 25px rgba(0,0,0,0.2)', border: '1px solid var(--color-glass-border)' }}>
      {/* Sidebar: Chats */}
      <div style={{ flex: '0 0 350px', display: 'flex', flexDirection: 'column', background: 'rgba(20,20,30,0.4)', borderRight: '1px solid var(--color-glass-border)', overflow: 'hidden' }}>
        
        {/* Sidebar Header */}
        <div style={{ padding: '1rem', borderBottom: '1px solid var(--color-glass-border)', background: 'rgba(0,0,0,0.2)', flexShrink: 0 }}>
          <h2 style={{ margin: '0 0 1rem 0', fontSize: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <MessageCircle size={20} /> Chats
          </h2>
          <div style={{ position: 'relative' }}>
            <Search size={16} style={{ position: 'absolute', top: '50%', left: '10px', transform: 'translateY(-50%)', opacity: 0.5 }} />
            <input 
               type="text" 
               className="glass-input" 
               placeholder="Search or start new chat" 
               value={searchQuery}
               onChange={(e) => setSearchQuery(e.target.value)}
               style={{ width: '100%', paddingLeft: '2.2rem', borderRadius: '20px', fontSize: '0.85rem' }}
            />
          </div>
        </div>
        
        {/* Chat List */}
        <div style={{ overflowY: 'auto', flex: 1 }}>
          {loadingContacts ? (
             <p style={{ textAlign: 'center', opacity: 0.5, marginTop: '2rem', fontSize: '0.9rem' }}>Loading chats...</p>
          ) : filteredChats.length === 0 ? (
             <p style={{ textAlign: 'center', opacity: 0.5, marginTop: '2rem', fontSize: '0.9rem' }}>No conversations found.</p>
          ) : (
            filteredChats.map((c, idx) => (
              <div 
                key={c.phone || idx}
                onClick={() => setSelectedPhone(c.phone)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  padding: '0.85rem 1rem',
                  cursor: 'pointer',
                  background: selectedPhone === c.phone ? 'rgba(255,255,255,0.08)' : 'transparent',
                  borderBottom: '1px solid rgba(255,255,255,0.03)',
                  transition: 'background 0.1s ease',
                }}
                onMouseEnter={(e) => {
                  if (selectedPhone !== c.phone) e.currentTarget.style.background = 'rgba(255,255,255,0.03)';
                }}
                onMouseLeave={(e) => {
                  if (selectedPhone !== c.phone) e.currentTarget.style.background = 'transparent';
                }}
              >
                <div style={{ 
                  width: '45px', height: '45px', borderRadius: '50%', 
                  background: 'var(--color-primary-glow)', 
                  display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                  marginRight: '1rem'
                }}>
                  <User size={24} style={{ color: 'var(--color-primary)' }}/>
                </div>
                <div style={{ overflow: 'hidden', flex: 1 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '0.2rem' }}>
                     <div style={{ fontWeight: '600', fontSize: '1rem', whiteSpace: 'nowrap', textOverflow: 'ellipsis', overflow: 'hidden' }}>
                       {c.customerName || c.phone}
                     </div>
                     <div style={{ fontSize: '0.75rem', opacity: 0.6, flexShrink: 0, marginLeft: '0.5rem' }}>
                       {formatTime(c.timestamp)}
                     </div>
                  </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div style={{ fontSize: '0.85rem', opacity: 0.6, whiteSpace: 'nowrap', textOverflow: 'ellipsis', overflow: 'hidden', flex: 1 }}>
                        {c.direction === 'OUT' ? '✓ ' : ''}{c.message || 'No messages yet'}
                      </div>
                      {c.requiresHuman && (
                        <span style={{ 
                          fontSize: '0.6rem', 
                          background: 'var(--color-accent)', 
                          color: '#fff', 
                          padding: '2px 6px', 
                          borderRadius: '10px', 
                          fontWeight: 'bold',
                          marginLeft: '0.5rem',
                          boxShadow: '0 0 10px var(--color-accent)'
                        }}>
                          NEEDS AGENT
                        </span>
                      )}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Main Conversation Pane */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: 'rgba(10,10,15,0.8)', position: 'relative', minWidth: 0, overflow: 'hidden' }}>
        {/* Background Pattern (Optional subtle generic texture) */}
        <div style={{ position: 'absolute', inset: 0, opacity: 0.03, pointerEvents: 'none', backgroundImage: 'radial-gradient(var(--color-primary) 1px, transparent 1px)', backgroundSize: '20px 20px' }}></div>
        
        {!selectedPhone ? (
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', opacity: 0.4, zIndex: 1 }}>
             <MessageCircle size={80} style={{ marginBottom: '1.5rem', opacity: 0.3 }} />
             <h3 style={{ fontWeight: '400', marginBottom: '0.5rem' }}>WhatsApp AI SaaS</h3>
             <p style={{ fontSize: '0.9rem' }}>Select a chat to view your conversation history</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', height: '100%', zIndex: 1 }}>
            {/* Chat Header */}
            <div style={{ padding: '0.75rem 1.5rem', borderBottom: '1px solid var(--color-glass-border)', background: 'rgba(20,20,30,0.8)', display: 'flex', alignItems: 'center', gap: '1rem', backdropFilter: 'blur(10px)' }}>
               <div style={{ 
                  width: '40px', height: '40px', borderRadius: '50%', 
                  background: 'var(--color-primary-glow)', 
                  display: 'flex', alignItems: 'center', justifyContent: 'center' 
                }}>
                  <User size={20} style={{ color: 'var(--color-primary)' }}/>
                </div>
                <div style={{ flex: 1 }}>
                  <h3 style={{ margin: 0, fontSize: '1.05rem', fontWeight: '600', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    {activeChats.find(c => c.phone === selectedPhone)?.customerName || selectedPhone}
                    {activeChats.find(c => c.phone === selectedPhone)?.requiresHuman && (
                       <span style={{ fontSize: '0.65rem', background: 'var(--color-accent)', color: '#fff', padding: '2px 8px', borderRadius: '10px' }}>NEEDS AGENT</span>
                    )}
                  </h3>
                  <div style={{ fontSize: '0.8rem', opacity: 0.7 }}>
                    {activeChats.find(c => c.phone === selectedPhone)?.requiresHumanReason ? (
                      <span style={{ color: 'var(--color-accent)', fontWeight: '500' }}>Reason: {activeChats.find(c => c.phone === selectedPhone).requiresHumanReason}</span>
                    ) : (
                      activeChats.find(c => c.phone === selectedPhone)?.customerName ? selectedPhone : 'Customer'
                    )}
                  </div>
                </div>
                {activeChats.find(c => c.phone === selectedPhone)?.requiresHuman ? (
                  <button onClick={handleMarkHandled} className="glass-button success" style={{ padding: '0.4rem 0.8rem', fontSize: '0.75rem', height: 'auto', display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <ShieldCheck size={14} /> Enable AI Assistant
                  </button>
                ) : (
                  <button onClick={handleTakeOver} className="glass-button" style={{ padding: '0.4rem 0.8rem', fontSize: '0.75rem', height: 'auto', display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <ShieldOff size={14} /> Take Over (Human)
                  </button>
                )}
            </div>

            {/* Chat Messages */}
            <div style={{ flex: 1, overflowY: 'auto', padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              {loadingMessages ? (
                <div style={{ display: 'flex', justifyContent: 'center', padding: '2rem' }}>
                   <p style={{ opacity: 0.5, fontSize: '0.9rem', background: 'rgba(0,0,0,0.3)', padding: '0.5rem 1rem', borderRadius: '20px' }}>Loading history...</p>
                </div>
              ) : error ? (
                <p style={{ color: '#f87171', padding: '0.5rem', background: 'rgba(248,113,113,0.1)', borderRadius: 'var(--radius-sm)', textAlign: 'center' }}>⚠️ {error}</p>
              ) : messages.length === 0 ? (
                <div style={{ display: 'flex', justifyContent: 'center', padding: '2rem' }}>
                   <p style={{ opacity: 0.5, fontSize: '0.9rem', background: 'rgba(0,0,0,0.3)', padding: '0.5rem 1rem', borderRadius: '20px' }}>No messages found.</p>
                </div>
              ) : (
                messages.map((m, idx) => {
                  const showDatePill = idx === 0 || new Date(m.createdAt).toDateString() !== new Date(messages[idx-1].createdAt).toDateString();
                  return (
                    <div key={m.id || idx} style={{ display: 'flex', flexDirection: 'column' }}>
                      {showDatePill && (
                        <div style={{ display: 'flex', justifyContent: 'center', margin: '1rem 0 0.5rem 0' }}>
                           <span style={{ fontSize: '0.75rem', padding: '0.3rem 0.8rem', background: 'rgba(0,0,0,0.4)', borderRadius: '10px', opacity: 0.8 }}>
                             {new Date(m.createdAt).toLocaleDateString([], { weekday: 'long', month: 'short', day: 'numeric' })}
                           </span>
                        </div>
                      )}
                      <div style={{ 
                        display: 'flex', 
                        justifyContent: m.direction === 'IN' ? 'flex-start' : 'flex-end',
                        marginBottom: '0.2rem'
                      }}>
                        <div style={{ 
                          maxWidth: '75%', 
                          padding: '0.5rem 0.75rem', 
                          borderRadius: '8px',
                          borderTopLeftRadius: m.direction === 'IN' ? 0 : '8px',
                          borderTopRightRadius: m.direction === 'OUT' ? 0 : '8px',
                          background: m.direction === 'IN' ? 'rgba(40,40,45,0.9)' : 'var(--color-primary-glow)',
                          color: '#fff',
                          boxShadow: '0 1px 2px rgba(0,0,0,0.2)',
                          position: 'relative'
                        }}>
                          <div style={{ wordBreak: 'break-word', whiteSpace: 'pre-wrap', lineHeight: '1.4', fontSize: '0.95rem' }}>{m.content}</div>
                          <div style={{ 
                            fontSize: '0.65rem', 
                            opacity: 0.6, 
                            marginTop: '0.2rem', 
                            textAlign: 'right',
                            display: 'flex',
                            justifyContent: 'flex-end',
                            alignItems: 'center',
                            gap: '0.25rem'
                          }}>
                            {m.createdAt ? new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
                            {m.direction === 'OUT' && <span>✓✓</span>}
                          </div>
                          {m.guardrailTriggered && (
                            <div style={{ marginTop: '0.5rem', fontSize: '0.75rem', color: '#f87171', background: 'rgba(248,113,113,0.1)', padding: '0.25rem 0.5rem', borderRadius: '4px' }}>
                              ⚠️ Blocked: {m.guardrailReason}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Live Chat Input */}
            <div style={{ padding: '1rem', background: 'rgba(20,20,30,0.8)', borderTop: '1px solid var(--color-glass-border)', display: 'flex', gap: '1rem', alignItems: 'center' }}>
               <textarea 
                  placeholder={activeChats.find(c => c.phone === selectedPhone)?.requiresHuman ? "Type a message..." : "AI is active. Switch to Human mode to reply."}
                  disabled={!activeChats.find(c => c.phone === selectedPhone)?.requiresHuman}
                  className="glass-input" 
                  value={replyText}
                  onChange={(e) => setReplyText(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault();
                      handleSendReply();
                    }
                  }}
                  style={{ 
                    flex: 1, 
                    padding: '0.75rem 1.5rem', 
                    borderRadius: '25px', 
                    resize: 'none', 
                    height: '45px',
                    opacity: activeChats.find(c => c.phone === selectedPhone)?.requiresHuman ? 1 : 0.5,
                    cursor: activeChats.find(c => c.phone === selectedPhone)?.requiresHuman ? 'text' : 'not-allowed'
                  }} 
               />
               <button 
                  className="glass-button primary" 
                  disabled={!activeChats.find(c => c.phone === selectedPhone)?.requiresHuman || !replyText.trim() || sendingReply}
                  onClick={handleSendReply}
                  style={{ width: '45px', height: '45px', borderRadius: '50%', padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
               >
                  <Send size={18} />
               </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
