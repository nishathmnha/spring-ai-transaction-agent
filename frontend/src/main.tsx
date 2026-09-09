import React, { FormEvent, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Check, ChevronDown, ChevronRight, Send, X } from 'lucide-react';
import './styles.css';

type ToolTrace = {
  tool: string;
  startTime: string;
  durationMs: number;
  status: string;
  summary: string;
  errorCode?: string;
};

type PendingTransfer = {
  sourceAccount: string;
  sourceAccountAlias: string;
  destinationAccount: string;
  beneficiaryName: string;
  amount: number;
  currency: string;
  availableBalance: number;
  balanceAfterTransfer: number;
  uuid: string;
};

type Transaction = {
  transactionId: string;
  uuid: string;
  sourceAccount: string;
  destinationAccount: string;
  beneficiaryName: string;
  amount: number;
  currency: string;
  sourceBalanceAfter: number;
  destinationBalanceAfter: number;
  status: string;
};

type AgentResponse = {
  conversationId: string;
  status: string;
  message: string;
  confirmationRequired: boolean;
  transfer?: PendingTransfer;
  transaction?: Transaction;
  toolTrace: ToolTrace[];
};

type ChatItem = {
  role: 'user' | 'assistant';
  text: string;
  response?: AgentResponse;
};

const token = import.meta.env.VITE_DEMO_AUTH_TOKEN || 'demo-token';

function mask(account?: string) {
  return account ? `****${account.slice(-4)}` : '';
}

function money(value?: number, currency = 'LKR') {
  if (value === undefined || value === null) return '';
  return `${currency} ${Number(value).toFixed(2)}`;
}

function App() {
  const [conversationId] = useState(`CONV-${Date.now()}`);
  const [items, setItems] = useState<ChatItem[]>([]);
  const [message, setMessage] = useState('Transfer LKR 10 from my salary account to Varuni.');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const latestResponse = useMemo(() => [...items].reverse().find(i => i.response)?.response, [items]);

  async function call(path: string, body?: unknown) {
    const res = await fetch(path, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: body ? JSON.stringify(body) : undefined
    });
    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }
    return (await res.json()) as AgentResponse;
  }

  async function send(e: FormEvent) {
    e.preventDefault();
    if (!message.trim()) return;
    const text = message.trim();
    setItems(prev => [...prev, { role: 'user', text }]);
    setMessage('');
    setLoading(true);
    setError('');
    try {
      const response = await call('/api/agent/chat', { conversationId, message: text });
      setItems(prev => [...prev, { role: 'assistant', text: response.message, response }]);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Request failed');
    } finally {
      setLoading(false);
    }
  }

  async function confirm() {
    setLoading(true);
    setError('');
    try {
      const response = await call(`/api/agent/conversations/${conversationId}/confirm`);
      setItems(prev => [...prev, { role: 'assistant', text: response.message, response }]);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Confirmation failed');
    } finally {
      setLoading(false);
    }
  }

  async function reject() {
    setLoading(true);
    setError('');
    try {
      const response = await call(`/api/agent/conversations/${conversationId}/reject`);
      setItems(prev => [...prev, { role: 'assistant', text: response.message, response }]);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Rejection failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="shell">
      <section className="chat">
        <header>
          <div>
            <h1>Spring AI Banking Agent</h1>
            <p>{conversationId}</p>
          </div>
          <span className="status">{latestResponse?.status || 'READY'}</span>
        </header>

        <div className="history">
          {items.map((item, index) => (
            <article key={index} className={`bubble ${item.role}`}>
              <p>{item.text}</p>
              {item.response?.transfer && <TransferCard transfer={item.response.transfer} onConfirm={confirm} onReject={reject} disabled={loading} />}
              {item.response?.transaction && <TransactionCard transaction={item.response.transaction} />}
              {(item.response?.toolTrace?.length ?? 0) > 0 && <Trace trace={item.response!.toolTrace} />}
            </article>
          ))}
          {loading && <div className="loading">Processing...</div>}
          {error && <div className="error">{error}</div>}
        </div>

        <form onSubmit={send} className="composer">
          <input value={message} onChange={e => setMessage(e.target.value)} placeholder="Ask about a balance or transfer to a saved beneficiary" />
          <button type="submit" disabled={loading} aria-label="Send"><Send size={18} /></button>
        </form>
      </section>
    </main>
  );
}

function TransferCard({ transfer, onConfirm, onReject, disabled }: { transfer: PendingTransfer; onConfirm: () => void; onReject: () => void; disabled: boolean }) {
  return (
    <div className="transfer-card">
      <h2>Please confirm the transfer</h2>
      <dl>
        <dt>From</dt><dd>{transfer.sourceAccountAlias}</dd>
        <dt>To</dt><dd>{transfer.beneficiaryName}</dd>
        <dt>Destination account</dt><dd>{mask(transfer.destinationAccount)}</dd>
        <dt>Amount</dt><dd>{money(transfer.amount, transfer.currency)}</dd>
        <dt>Balance after transfer</dt><dd>{money(transfer.balanceAfterTransfer, transfer.currency)}</dd>
      </dl>
      <div className="actions">
        <button className="secondary" onClick={onReject} disabled={disabled} type="button"><X size={16} /> Reject</button>
        <button onClick={onConfirm} disabled={disabled} type="button"><Check size={16} /> Confirm Transfer</button>
      </div>
    </div>
  );
}

function TransactionCard({ transaction }: { transaction: Transaction }) {
  return (
    <div className="transaction-card">
      <strong>{transaction.status}</strong>
      <span>{transaction.transactionId}</span>
      <p>{money(transaction.amount, transaction.currency)} to {transaction.beneficiaryName}</p>
      <p>Source balance: {money(transaction.sourceBalanceAfter, transaction.currency)}</p>
      <p>Destination: {mask(transaction.destinationAccount)}</p>
    </div>
  );
}

function Trace({ trace }: { trace: ToolTrace[] }) {
  const [open, setOpen] = useState(false);
  return (
    <div className="trace">
      <button type="button" onClick={() => setOpen(!open)} className="trace-toggle">
        {open ? <ChevronDown size={16} /> : <ChevronRight size={16} />} Tool trace ({trace.length})
      </button>
      {open && (
        <div className="trace-list">
          {trace.map((item, index) => (
            <div key={index} className="trace-row">
              <strong>{item.tool}</strong>
              <span>{item.status}</span>
              <span>{item.durationMs} ms</span>
              <p>{item.summary}</p>
              {item.errorCode && <code>{item.errorCode}</code>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

createRoot(document.getElementById('root')!).render(<App />);
