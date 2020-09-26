export function buildCitation(book, chapter) {
  if (!book || !chapter || book.citationTemplate === null || chapter.citationParameter === null) {
    return null;
  }

  return `{{${book.citationTemplate}|${chapter.citationParameter}}}`;
}

export function copyText(text) {
  const el = document.createElement('textarea');
  el.value = text;
  document.body.appendChild(el);
  el.select();

  document.execCommand('copy');

  document.body.removeChild(el);
}

export function saveAs(blob, name) {
  const a = document.createElement('a');
  a.download = name;
  a.rel = 'noopener';
  a.href = URL.createObjectURL(blob);
  setTimeout(() => {
    URL.revokeObjectURL(a.href);
  }, 4e4);
  setTimeout(() => {
    a.click();
  }, 0);
}
