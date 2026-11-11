const API = {
  tickets: '/chamados',
  clients: '/clientes',
  categories: '/categorias',
  technicians: '/tecnicos'
};

const STATUS_LABELS = {
  ABERTO: 'Aberto',
  EM_ATENDIMENTO: 'Em atendimento',
  AGUARDANDO_CLIENTE: 'Aguardando cliente',
  FECHADO: 'Fechado'
};
const PRIORITY_LABELS = { BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta', CRITICA: 'Crítica' };
const STATUS_TRANSITIONS = {
  ABERTO: ['EM_ATENDIMENTO'],
  EM_ATENDIMENTO: ['AGUARDANDO_CLIENTE', 'FECHADO'],
  AGUARDANDO_CLIENTE: ['EM_ATENDIMENTO', 'FECHADO'],
  FECHADO: []
};
const CATALOGS = {
  clients: {
    title: 'Clientes',
    description: 'Pessoas que podem abrir chamados.',
    endpoint: API.clients,
    columns: [['nome', 'Nome'], ['email', 'E-mail'], ['telefone', 'Telefone']],
    fields: [
      { name: 'nome', label: 'Nome', required: true, maxLength: 100 },
      { name: 'email', label: 'E-mail', type: 'email', required: true, maxLength: 120 },
      { name: 'telefone', label: 'Telefone', maxLength: 20 }
    ]
  },
  categories: {
    title: 'Categorias',
    description: 'Tipos de assunto usados para organizar os chamados.',
    endpoint: API.categories,
    columns: [['nome', 'Categoria'], ['descricao', 'Descrição']],
    fields: [
      { name: 'nome', label: 'Nome', required: true, maxLength: 100 },
      { name: 'descricao', label: 'Descrição', type: 'textarea', maxLength: 500 }
    ]
  },
  technicians: {
    title: 'Técnicos',
    description: 'Equipe responsável pelo atendimento.',
    endpoint: API.technicians,
    columns: [['nome', 'Nome'], ['email', 'E-mail'], ['perfil', 'Perfil', 'perfil']],
    fields: [
      { name: 'nome', label: 'Nome', required: true, maxLength: 100 },
      { name: 'email', label: 'E-mail', type: 'email', required: true, maxLength: 120 },
      { name: 'perfil', label: 'Perfil', type: 'select', required: true, options: [['TECNICO', 'Técnico'], ['ADMIN', 'Administrador']] }
    ]
  }
};

const state = {
  page: 'tickets',
  catalog: 'clients',
  tickets: [],
  clients: [],
  categories: [],
  technicians: [],
  pageNumber: 0,
  pageSize: 12,
  totalPages: 0,
  totalElements: 0,
  activeTicket: null,
  editingCatalogId: null,
  noticeTimeout: null
};

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, char => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
})[char]);
const label = (map, value) => map[value] || value || '—';

async function request(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers
    }
  });
  if (!response.ok) {
    let message = `Não foi possível concluir a operação (HTTP ${response.status}).`;
    try {
      const body = await response.json();
      if (body.erro) message = body.detalhes?.length ? `${body.erro}: ${body.detalhes.join('; ')}` : body.erro;
      else if (body.message) message = body.message;
    } catch (error) {
      if (!(error instanceof SyntaxError)) throw error;
    }
    throw new Error(message);
  }
  if (response.status === 204) return null;
  return response.json();
}

function showNotice(message, type = 'success') {
  const notice = $('#notice');
  notice.textContent = message;
  notice.className = `notice ${type}`;
  notice.hidden = false;
  clearTimeout(state.noticeTimeout);
  state.noticeTimeout = setTimeout(() => { notice.hidden = true; }, 5500);
}

function goTo(page) {
  state.page = page;
  const titles = { tickets: 'Chamados', 'new-ticket': 'Novo chamado', catalogs: 'Cadastros' };
  $('#page-title').textContent = titles[page];
  $$('.page-section').forEach(section => {
    section.hidden = section.id !== `${page}-page`;
  });
  $$('.nav-link').forEach(button => button.classList.toggle('active', button.dataset.page === page));
  if (page === 'catalogs') renderCatalog();
  if (page === 'new-ticket') populateTicketOptions();
}

function displayDate(value) {
  if (!value) return '—';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short'
  }).format(parsed);
}

function ticketName(list, id) {
  return list.find(item => item.id === id)?.nome || `Cadastro #${id ?? '—'}`;
}

function buildTicketQuery(page = 0) {
  const params = new URLSearchParams({ page, size: state.pageSize, sort: 'dataAbertura,desc' });
  if ($('#filter-status').value) params.set('status', $('#filter-status').value);
  if ($('#filter-priority').value) params.set('prioridade', $('#filter-priority').value);
  if ($('#filter-client').value) params.set('clienteId', $('#filter-client').value);
  return `${API.tickets}?${params}`;
}

async function loadTickets(page = state.pageNumber) {
  state.pageNumber = page;
  const result = await request(buildTicketQuery(page));
  state.tickets = result.content || [];
  state.totalPages = result.totalPages || 0;
  state.totalElements = result.totalElements || 0;
  renderTickets();
}

async function loadReferenceData() {
  const [clients, categories, technicians] = await Promise.all([
    request(API.clients),
    request(API.categories),
    request(API.technicians)
  ]);
  state.clients = clients;
  state.categories = categories;
  state.technicians = technicians;
  populateTicketOptions();
  renderCatalog();
  renderTickets();
}

async function refresh() {
  try {
    await Promise.all([loadReferenceData(), loadTickets(0)]);
  } catch (error) {
    showNotice(`${error.message} Confira se a aplicação está iniciada e tente novamente.`, 'error');
    $('#ticket-count').textContent = 'Não foi possível carregar';
  }
}

function renderTickets() {
  const query = $('#ticket-search').value.trim().toLocaleLowerCase('pt-BR');
  const tickets = state.tickets.filter(ticket =>
    !query || `${ticket.titulo} ${ticket.descricao}`.toLocaleLowerCase('pt-BR').includes(query));
  const grid = $('#tickets-grid');
  grid.innerHTML = tickets.map(ticket => `
    <button class="ticket-card" type="button" data-ticket-id="${ticket.id}"
      data-status="${escapeHtml(ticket.status)}" data-priority="${escapeHtml(ticket.prioridade)}"
      aria-label="Abrir chamado ${ticket.id}: ${escapeHtml(ticket.titulo)}">
      <div class="ticket-topline">
        <span class="ticket-id">CH-${String(ticket.id).padStart(4, '0')}</span>
        <span class="badge badge-status-${escapeHtml(ticket.status?.toLowerCase())}">${escapeHtml(label(STATUS_LABELS, ticket.status))}</span>
      </div>
      <h3>${escapeHtml(ticket.titulo)}</h3>
      <p class="ticket-description">${escapeHtml(ticket.descricao)}</p>
      <div class="ticket-meta">
        <span><strong>${escapeHtml(ticketName(state.clients, ticket.clienteId))}</strong></span>
        <span>${escapeHtml(displayDate(ticket.dataAbertura))}</span>
      </div>
    </button>
  `).join('');
  $('#empty-tickets').hidden = tickets.length > 0;
  $('#tickets-grid').hidden = tickets.length === 0;
  $('#ticket-count').textContent = `${state.totalElements} ${state.totalElements === 1 ? 'chamado' : 'chamados'}`;
  $('#page-label').textContent = state.totalPages ? `Página ${state.pageNumber + 1} de ${state.totalPages}` : 'Página 0';
  $('#previous-page').disabled = state.pageNumber <= 0;
  $('#next-page').disabled = state.totalPages === 0 || state.pageNumber + 1 >= state.totalPages;
}

function populateSelect(selector, items, placeholder) {
  const select = $(selector);
  const selected = select.value;
  select.innerHTML = `<option value="">${escapeHtml(placeholder)}</option>` + items
    .map(item => `<option value="${item.id}">${escapeHtml(item.nome)}</option>`).join('');
  if (items.some(item => String(item.id) === selected)) select.value = selected;
}

function populateTicketOptions() {
  populateSelect('#ticket-client', state.clients, state.clients.length ? 'Selecione um cliente' : 'Cadastre um cliente primeiro');
  populateSelect('#filter-client', state.clients, 'Todos os clientes');
  populateSelect('#ticket-category', state.categories, state.categories.length ? 'Selecione uma categoria' : 'Cadastre uma categoria primeiro');
  populateSelect('#ticket-technician', state.technicians, state.technicians.length ? 'Selecione um técnico' : 'Cadastre um técnico primeiro');
}

function catalogValue(item, field) {
  const value = item[field];
  if (field === 'perfil') return value === 'ADMIN' ? 'Administrador' : 'Técnico';
  return value || '—';
}

function renderCatalog() {
  const config = CATALOGS[state.catalog];
  if (!config) return;
  $('#catalog-title').textContent = config.title;
  $('#catalog-description').textContent = config.description;
  $$('.catalog-tab').forEach(button => button.classList.toggle('active', button.dataset.catalog === state.catalog));
  $('#catalog-table-head').innerHTML = `<tr>${config.columns.map(([, title]) => `<th scope="col">${escapeHtml(title)}</th>`).join('')}<th scope="col"><span class="sr-only">Ações</span></th></tr>`;
  const items = state[state.catalog];
  $('#catalog-table-body').innerHTML = items.map(item => `
    <tr>
      ${config.columns.map(([field]) => `<td>${escapeHtml(catalogValue(item, field))}</td>`).join('')}
      <td><div class="row-actions">
        <button class="button button-secondary" type="button" data-edit-item="${item.id}">Editar</button>
        <button class="button button-danger" type="button" data-delete-item="${item.id}">Excluir</button>
      </div></td>
    </tr>
  `).join('');
  $('#empty-catalog').hidden = items.length > 0;
  $('.table-wrap').hidden = items.length === 0;
}

function openTicketDialog(ticket) {
  state.activeTicket = ticket;
  $('#detail-reference').textContent = `CH-${String(ticket.id).padStart(4, '0')} · ABERTO EM ${displayDate(ticket.dataAbertura)}`;
  $('#detail-title').textContent = ticket.titulo;
  const commentList = ticket.comentarios?.length
    ? `<ul class="comment-list">${ticket.comentarios.map(comment => `
      <li class="comment-item">
        <div class="comment-heading"><strong>${escapeHtml(comment.autor)}</strong><time>${escapeHtml(displayDate(comment.dataCriacao))}</time></div>
        <p>${escapeHtml(comment.texto)}</p>
      </li>`).join('')}</ul>`
    : '<p class="no-comments">Ainda não há atualizações neste chamado.</p>';
  const transitions = STATUS_TRANSITIONS[ticket.status] || [];
  const statusControl = transitions.length ? `
    <label class="field">Alterar status
      <select id="detail-status">${transitions.map(status => `<option value="${status}">${escapeHtml(STATUS_LABELS[status])}</option>`).join('')}</select>
    </label>
    <button class="button button-secondary" id="save-status" type="button">Atualizar status</button>
  ` : '<p class="form-hint">Este chamado está fechado.</p>';
  const technicianControl = ticket.status === 'FECHADO' ? '' : `
    <label class="field">Atribuir técnico
      <select id="detail-technician">${state.technicians.map(tech => `<option value="${tech.id}" ${tech.id === ticket.tecnicoId ? 'selected' : ''}>${escapeHtml(tech.nome)}</option>`).join('')}</select>
    </label>
    <button class="button button-secondary" id="save-technician" type="button">Atribuir técnico</button>
  `;
  $('#detail-content').innerHTML = `
    <div class="detail-badges">
      <span class="badge badge-status-${escapeHtml(ticket.status?.toLowerCase())}">${escapeHtml(label(STATUS_LABELS, ticket.status))}</span>
      <span class="badge badge-priority badge-priority-${escapeHtml(ticket.prioridade?.toLowerCase())}">Prioridade ${escapeHtml(label(PRIORITY_LABELS, ticket.prioridade))}</span>
    </div>
    <p class="detail-description">${escapeHtml(ticket.descricao)}</p>
    <div class="detail-facts">
      <div><span class="fact-label">Cliente</span><span class="fact-value">${escapeHtml(ticketName(state.clients, ticket.clienteId))}</span></div>
      <div><span class="fact-label">Categoria</span><span class="fact-value">${escapeHtml(ticketName(state.categories, ticket.categoriaId))}</span></div>
      <div><span class="fact-label">Técnico</span><span class="fact-value">${escapeHtml(ticketName(state.technicians, ticket.tecnicoId))}</span></div>
      <div><span class="fact-label">Encerrado em</span><span class="fact-value">${escapeHtml(displayDate(ticket.dataFechamento))}</span></div>
    </div>
    <section class="detail-section">
      <h3>Histórico de atualizações</h3>
      ${commentList}
      <form class="comment-form" id="comment-form">
        <label>Autor<input name="autor" maxlength="100" required placeholder="Seu nome"></label>
        <label>Comentário<input name="texto" maxlength="2000" required placeholder="Escreva uma atualização"></label>
        <button class="button button-primary" type="submit">Enviar</button>
      </form>
    </section>
    <div class="detail-actions">${statusControl}${technicianControl}</div>
  `;
  const dialog = $('#ticket-dialog');
  if (!dialog.open) dialog.showModal();
}

function fieldMarkup(field, value = '') {
  const required = field.required ? 'required' : '';
  const maxLength = field.maxLength ? `maxlength="${field.maxLength}"` : '';
  const escapedValue = escapeHtml(value);
  let control;
  if (field.type === 'select') {
    control = `<select name="${field.name}" ${required}><option value="">Selecione</option>${field.options.map(([key, text]) => `<option value="${key}" ${value === key ? 'selected' : ''}>${text}</option>`).join('')}</select>`;
  } else if (field.type === 'textarea') {
    control = `<textarea name="${field.name}" rows="4" ${maxLength} ${required}>${escapedValue}</textarea>`;
  } else {
    control = `<input name="${field.name}" type="${field.type || 'text'}" value="${escapedValue}" ${maxLength} ${required}>`;
  }
  return `<label>${escapeHtml(field.label)}${control}</label>`;
}

function openCatalogDialog(item = null) {
  const config = CATALOGS[state.catalog];
  state.editingCatalogId = item?.id ?? null;
  $('#catalog-dialog-title').textContent = item ? `Editar ${config.title.toLowerCase().slice(0, -1)}` : `Novo cadastro · ${config.title}`;
  $('#catalog-form').innerHTML = config.fields.map(field => fieldMarkup(field, item?.[field.name] ?? '')).join('')
    + `<button class="button button-primary" type="submit">${item ? 'Salvar alterações' : 'Criar cadastro'}</button>`;
  $('#catalog-dialog').showModal();
}

async function saveNewTicket(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const values = Object.fromEntries(new FormData(form));
  values.clienteId = Number(values.clienteId);
  values.categoriaId = Number(values.categoriaId);
  values.tecnicoId = Number(values.tecnicoId);
  try {
    await request(API.tickets, { method: 'POST', body: JSON.stringify(values) });
    form.reset();
    showNotice('Chamado criado com sucesso.');
    goTo('tickets');
    await loadTickets(0);
  } catch (error) {
    showNotice(error.message, 'error');
  }
}

async function saveCatalogItem(event) {
  event.preventDefault();
  const config = CATALOGS[state.catalog];
  const values = Object.fromEntries(new FormData(event.currentTarget));
  const path = state.editingCatalogId ? `${config.endpoint}/${state.editingCatalogId}` : config.endpoint;
  try {
    await request(path, { method: state.editingCatalogId ? 'PUT' : 'POST', body: JSON.stringify(values) });
    $('#catalog-dialog').close();
    await loadReferenceData();
    showNotice(`${config.title.slice(0, -1)} ${state.editingCatalogId ? 'atualizado' : 'criado'} com sucesso.`);
    state.editingCatalogId = null;
  } catch (error) {
    showNotice(error.message, 'error');
  }
}

async function deleteCatalogItem(id) {
  const config = CATALOGS[state.catalog];
  const item = state[state.catalog].find(entry => entry.id === id);
  if (!item || !window.confirm(`Excluir "${item.nome}"?`)) return;
  try {
    await request(`${config.endpoint}/${id}`, { method: 'DELETE' });
    await loadReferenceData();
    showNotice('Cadastro excluído com sucesso.');
  } catch (error) {
    showNotice(error.message, 'error');
  }
}

async function updateTicketAction(path, body, successMessage) {
  try {
    await request(path, { method: 'PATCH', body: JSON.stringify(body) });
    const updated = await request(`${API.tickets}/${state.activeTicket.id}`);
    state.activeTicket = updated;
    await loadTickets(state.pageNumber);
    openTicketDialog(updated);
    showNotice(successMessage);
  } catch (error) {
    showNotice(error.message, 'error');
  }
}

async function addComment(event) {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.currentTarget));
  try {
    await request(`${API.tickets}/${state.activeTicket.id}/comentarios`, {
      method: 'POST',
      body: JSON.stringify(values)
    });
    const updated = await request(`${API.tickets}/${state.activeTicket.id}`);
    state.activeTicket = updated;
    openTicketDialog(updated);
    showNotice('Atualização adicionada ao histórico.');
  } catch (error) {
    showNotice(error.message, 'error');
  }
}

document.addEventListener('click', async event => {
  const pageButton = event.target.closest('[data-page], [data-go]');
  if (pageButton) {
    goTo(pageButton.dataset.page || pageButton.dataset.go);
    return;
  }
  const closeButton = event.target.closest('[data-close-dialog]');
  if (closeButton) {
    closeButton.closest('dialog').close();
    return;
  }
  const ticketCard = event.target.closest('[data-ticket-id]');
  if (ticketCard) {
    try {
      openTicketDialog(await request(`${API.tickets}/${ticketCard.dataset.ticketId}`));
    } catch (error) {
      showNotice(error.message, 'error');
    }
    return;
  }
  const catalogTab = event.target.closest('[data-catalog]');
  if (catalogTab) {
    state.catalog = catalogTab.dataset.catalog;
    renderCatalog();
    return;
  }
  const editButton = event.target.closest('[data-edit-item]');
  if (editButton) {
    const item = state[state.catalog].find(entry => entry.id === Number(editButton.dataset.editItem));
    openCatalogDialog(item);
    return;
  }
  const deleteButton = event.target.closest('[data-delete-item]');
  if (deleteButton) {
    await deleteCatalogItem(Number(deleteButton.dataset.deleteItem));
    return;
  }
  if (event.target.closest('#add-catalog-item')) {
    openCatalogDialog();
    return;
  }
  if (event.target.closest('#save-status')) {
    const status = $('#detail-status').value;
    await updateTicketAction(`${API.tickets}/${state.activeTicket.id}/status`, { status }, 'Status atualizado.');
    return;
  }
  if (event.target.closest('#save-technician')) {
    const tecnicoId = Number($('#detail-technician').value);
    await updateTicketAction(`${API.tickets}/${state.activeTicket.id}/atribuir`, { tecnicoId }, 'Técnico atribuído.');
  }
});

document.addEventListener('submit', async event => {
  if (event.target.id === 'ticket-filters') {
    event.preventDefault();
    try {
      await loadTickets(0);
    } catch (error) {
      showNotice(error.message, 'error');
    }
  } else if (event.target.id === 'new-ticket-form') {
    await saveNewTicket(event);
  } else if (event.target.id === 'catalog-form') {
    await saveCatalogItem(event);
  } else if (event.target.id === 'comment-form') {
    await addComment(event);
  }
});

$('#ticket-search').addEventListener('input', renderTickets);
$('#previous-page').addEventListener('click', () => loadTickets(state.pageNumber - 1).catch(error => showNotice(error.message, 'error')));
$('#next-page').addEventListener('click', () => loadTickets(state.pageNumber + 1).catch(error => showNotice(error.message, 'error')));
$$('dialog').forEach(dialog => dialog.addEventListener('click', event => {
  if (event.target === dialog) dialog.close();
}));

goTo('tickets');
refresh();
