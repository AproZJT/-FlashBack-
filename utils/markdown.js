function escapeHtml(text = '') {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function highlightCode(code = '') {
  const escaped = escapeHtml(code);
  return escaped
    .replace(/\b(const|let|var|function|return|if|else|for|while|switch|case|break|continue|class|new|import|from|export|async|await|try|catch|throw)\b/g, '<span class="md-k">$1</span>')
    .replace(/\b(true|false|null|undefined|NaN)\b/g, '<span class="md-b">$1</span>')
    .replace(/(["'`])([^"'`\n]*?)\1/g, '<span class="md-s">$&</span>')
    .replace(/\b(\d+(?:\.\d+)?)\b/g, '<span class="md-n">$1</span>')
    .replace(/(\/\/[^\n]*)/g, '<span class="md-c">$1</span>');
}

export function renderMarkdownToHtml(markdown = '') {
  const source = String(markdown || '');
  const codeBlocks = [];

  let html = source.replace(/```([a-zA-Z0-9_-]+)?\n?([\s\S]*?)```/g, (_, lang = '', code = '') => {
    const token = `__CODE_BLOCK_${codeBlocks.length}__`;
    codeBlocks.push(
      `<pre class="md-code"><div class="md-code-lang">${escapeHtml(lang || 'code')}</div><code>${highlightCode(code.trim())}</code></pre>`
    );
    return token;
  });

  html = escapeHtml(html);
  html = html.replace(/^###\s+(.*)$/gm, '<h3>$1</h3>');
  html = html.replace(/^##\s+(.*)$/gm, '<h2>$1</h2>');
  html = html.replace(/^#\s+(.*)$/gm, '<h1>$1</h1>');
  html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/`([^`]+)`/g, '<code class="md-inline-code">$1</code>');

  html = html
    .split(/\n{2,}/)
    .map(block => {
      if (/^__CODE_BLOCK_\d+__$/.test(block.trim())) return block.trim();
      return `<p>${block.replace(/\n/g, '<br/>')}</p>`;
    })
    .join('');

  codeBlocks.forEach((block, idx) => {
    html = html.replace(`__CODE_BLOCK_${idx}__`, block);
  });

  return html;
}
