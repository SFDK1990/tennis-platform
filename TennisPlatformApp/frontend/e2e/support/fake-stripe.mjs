// Stripe, for the E2E suite (42-fase27.5-analisis-pago-online.md). Playwright starts it on the
// host; the backend in Docker reaches it through compose.e2e.yaml. It answers the one call the
// backend makes, opening a Checkout page, and serves that page: "Pagar" sends the signed notice
// Stripe would send and takes the student back, "Volver sin pagar" only takes them back.
//
// The notice goes through the frontend's /api, as it does in production, so a body changed on
// the way would break its signature. The way back lands on the suite's frontend: the stack
// thinks it is on 3000, the suite runs it on 3100.
import { createHmac, randomUUID } from "node:crypto";
import { createServer } from "node:http";

const PORT = Number(process.env.FAKE_STRIPE_PORT || 12111);
const WEBHOOK_SECRET = "whsec_e2e";
const SECRET_KEY = "sk_test_e2e";
const FRONTEND = process.env.FAKE_STRIPE_FRONTEND || "http://localhost:3100";

/** @type {Map<string, { amount: number, currency: string, success: string, cancel: string }>} */
const sessions = new Map();

function back(url) {
  const target = new URL(url);
  return new URL(target.pathname + target.search, FRONTEND).toString();
}

async function readBody(request) {
  const chunks = [];
  for await (const chunk of request) chunks.push(chunk);
  return Buffer.concat(chunks).toString("utf8");
}

async function notify(id, session) {
  const body = JSON.stringify({
    id: `evt_e2e_${id}`,
    object: "event",
    type: "checkout.session.completed",
    data: { object: { id, object: "checkout.session", amount_total: session.amount, currency: session.currency, payment_status: "paid" } },
  });
  const t = Math.floor(Date.now() / 1000);
  const v1 = createHmac("sha256", WEBHOOK_SECRET).update(`${t}.${body}`).digest("hex");
  const answer = await fetch(`${FRONTEND}/api/v1/payments/stripe/webhook`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "Stripe-Signature": `t=${t},v1=${v1}` },
    body,
  });
  if (!answer.ok) throw new Error(`The backend refused the notice: ${answer.status}`);
}

createServer(async (request, response) => {
  const url = new URL(request.url ?? "/", `http://localhost:${PORT}`);
  try {
    if (request.method === "GET" && url.pathname === "/health") {
      response.writeHead(200).end("ok");
      return;
    }
    if (request.method === "POST" && url.pathname === "/v1/checkout/sessions") {
      if (request.headers.authorization !== `Bearer ${SECRET_KEY}`) {
        response.writeHead(401, { "Content-Type": "application/json" }).end('{"error":{"type":"invalid_request_error"}}');
        return;
      }
      const form = new URLSearchParams(await readBody(request));
      // Unique across runs, as Stripe's are: the stack keeps the purchases of earlier ones.
      const id = `cs_test_e2e_${randomUUID().replaceAll("-", "")}`;
      sessions.set(id, {
        amount: Number(form.get("line_items[0][price_data][unit_amount]")),
        currency: form.get("line_items[0][price_data][currency]") ?? "",
        success: form.get("success_url") ?? "",
        cancel: form.get("cancel_url") ?? "",
      });
      response.writeHead(200, { "Content-Type": "application/json" })
        .end(JSON.stringify({ id, object: "checkout.session", url: `http://localhost:${PORT}/pay/${id}` }));
      return;
    }
    const paying = url.pathname.match(/^\/pay\/([\w-]+)(\/cancel)?$/);
    const session = paying ? sessions.get(paying[1]) : undefined;
    if (paying && session && request.method === "GET" && !paying[2]) {
      response.writeHead(200, { "Content-Type": "text/html; charset=utf-8" }).end(`<!doctype html>
<html lang="es"><head><meta charset="utf-8"><title>Stripe de pruebas</title></head><body>
<h1>Stripe de pruebas</h1><p>${(session.amount / 100).toFixed(2)} ${session.currency.toUpperCase()}</p>
<form method="post" action="/pay/${paying[1]}"><button>Pagar</button></form>
<a href="/pay/${paying[1]}/cancel">Volver sin pagar</a></body></html>`);
      return;
    }
    if (paying && session && request.method === "POST" && !paying[2]) {
      await notify(paying[1], session);
      response.writeHead(303, { Location: back(session.success) }).end();
      return;
    }
    if (paying && session && paying[2]) {
      response.writeHead(303, { Location: back(session.cancel) }).end();
      return;
    }
    response.writeHead(404).end();
  } catch (error) {
    console.error(error);
    response.writeHead(500).end(String(error));
  }
}).listen(PORT, () => console.log(`Fake Stripe on http://localhost:${PORT}`));
