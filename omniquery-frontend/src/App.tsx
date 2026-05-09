import { useState } from 'react';
import './App.css';

type QueryTrace = {
  phase: string;
  message: string;
  detail: unknown;
};

type QueryResponse = {
  sessionId: string | null;
  turnId: string | null;
  mode: string | null;
  success: boolean;
  answer: string | null;
  rawSql: string | null;
  guardedSql: string | null;
  rows: Record<string, unknown>[];
  error: string | null;
  trace: QueryTrace[];
};

function App() {
  const [question, setQuestion] = useState('show recent orders with customer names');
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [response, setResponse] = useState<QueryResponse | null>(null);
  const [loading, setLoading] = useState(false);

  async function runQuery() {
    setLoading(true);
    try {
      const res = await fetch('http://localhost:8080/api/query', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question, tenantId: 'tenant_a', sessionId }),
      });
      const nextResponse = await res.json();
      setResponse(nextResponse);
      setSessionId(nextResponse.sessionId);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="page">
      <section className="shell">
        <header>
          <h1>OmniQuery AI</h1>
          <p>Generic NL2SQL engine with RAG, SQL guard, ACL rewrite, and trace.</p>
        </header>

        <div className="query-bar">
          <input value={question} onChange={(event) => setQuestion(event.target.value)} />
          <button onClick={runQuery} disabled={loading || !question.trim()}>
            {loading ? 'Running' : 'Run'}
          </button>
        </div>

        {response && (
          <div className="result-grid">
            <section>
              <h2>SQL</h2>
              <p>{response.mode || 'NEW_QUERY'}</p>
              <pre>{response.guardedSql || response.rawSql || response.error}</pre>
            </section>
            <section>
              <h2>Trace</h2>
              <ol>
                {response.trace.map((step, index) => (
                  <li key={`${step.phase}-${index}`}>
                    <strong>{step.phase}</strong>
                    <span>{step.message}</span>
                  </li>
                ))}
              </ol>
            </section>
            <section className="wide">
              <h2>Rows</h2>
              <pre>{JSON.stringify(response.rows, null, 2)}</pre>
            </section>
          </div>
        )}
      </section>
    </main>
  );
}

export default App;
