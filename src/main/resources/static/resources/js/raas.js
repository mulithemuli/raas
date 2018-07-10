(function($) {
	let agents = {
		'Merlin': { visible: false, agent: null},
		'Links': { visible: false, agent: null},
		'Genius': { visible: false, agent: null},
		'Genie': { visible: false, agent: null},
		'Rover': { visible: false, agent: null},
		'Peedy': { visible: false, agent: null},
		'Bonzi': { visible: false, agent: null},
		'Clippy': { visible: false, agent: null},
		'F1': { visible: false, agent: null},
		'Rocky': { visible: false, agent: null}
		};
	
	let defaults = {
			regex: '([a-zA-Z0-9_@:!]){4}([a-zA-Z]){2}([0-9]){2}',
			numTexts: 5,
			shareData: 'false',
			agents: []
	}
	
	// fields / elements
	let regexp = $(document.getElementById('regexp'));
	let texts = $(document.getElementById('texts'));
	let regexList = $(document.getElementById('regex_list'));
	let regexDialog = $(document.getElementById('regex_dialog'));
	let shareRegexCheck = $(document.getElementById('share_regex'));
	let lastUsedRegexPopupToggler = $('span.modal-toggler', document.getElementById('last_used_regex'));
	let lastUsedRegexInput = $('input', document.getElementById('last_used_regex'));
	let regexStatsList = $(document.getElementById('regex_stat_list'));
	let regexStatsDialog = $(document.getElementById('regex_stats_dialog'));
	let agentDropdown = $(document.getElementById('agent_dropdown'));
	
	let stompClient;
	let usedAgent;
	
	// templates
	let templates = {
			texts: _.template('\
<div class="row">\
	<div class="input-group col-md-12 mb-3">\
		<input type="text" class="form-control" readonly>\
		<div class="input-group-append">\
			<button type="button" class="btn btn-secondary" data-toggle="tooltip" data-placement="top" title="Copy"><i class="fas fa-clipboard"></i><span class="sr-only">Copy</span></button>\
		</div>\
	</div>\
</div>'),
			regexListItem: _.template('\
<div class="list-group-item list-group-item-action">\
	<div class="row">\
		<a href="#" class="col-sm-10"><%- regex %></a>\
		<div class="col-sm-2"><button type="button" class="btn btn-outline-danger btn-sm float-right" data-toggle="tooltip" data-placement="top" title="Delete"><i class="fas fa-trash-alt"></i></button></div>\
	</div>\
</div>'),
			lastUsedRegex: _.template('<a href="#" data-regex="<%- regex %>" data-toggle="tooltip" data-placement="top" title="Use this Regex"><%- regex %></a>'),
			regexStatsItem: _.template('<a href="#" class="list-group-item list-group-item-action" data-toggle="tooltip" data-placement="top" data-regex="<%- regex %>"><%- regex %> <span class="badge badge-secondary"><%- used %></span></a>'),
			agent: _.template('<a class="dropdown-item" href="#"><%- name %></a>')
	};
	
	let settings = {
			get lastRegex() {
				return localStorage.getItem('last_regex') || defaults.regex;
			},
			set lastRegex(lastRegex) {
				localStorage.setItem('last_regex', lastRegex);
			},
			get numTexts() {
				return parseInt(localStorage.getItem('num_texts') || defaults.numTexts, 10);
			},
			set numTexts(numTexts) {
				localStorage.setItem('num_texts', numTexts);
			},
			get storedRegex() {
				return JSON.parse(localStorage.getItem('stored_regex') || '{}');
			},
			set storedRegex(storedRegex) {
				localStorage.setItem('stored_regex', JSON.stringify(storedRegex));
			},
			get shareRegex() {
				return (localStorage.getItem('share_regex') || defaults.shareData) === 'true';
			},
			set shareRegex(shareRegex) {
				localStorage.setItem('share_regex', shareRegex);
			},
			get agents() {
				return JSON.parse(localStorage.getItem('agent')) || defaults.agents;
			},
			set agents(agents) {
				localStorage.setItem('agent', JSON.stringify(agents));
			}
	}
	
	// functions
	let updateRegex = (ex) => {
		regexp.val(ex);
	}
	
	let initTexts = () => {
		let count = settings.numTexts;
		for (let i = 0; i < count; i++) {
			texts.append(templates.texts());
		}
	}
	
	let playAgents = (animation) => {
		$.each(agents, (k, v) => {
			if (v.agent && animation) {
				v.agent.play(animation);
			} else if (v.agent) {
				v.agent.stop();
			}
		});
	}
	
	let generateTexts = (e) => {
		if (e) {
			e.preventDefault();
		}
		regexp.removeClass('is-invalid');
		let regex = regexp.val() || settings.lastRegex;
		try {
			playAgents();
			let randExp = new RandExp(regex);
			playAgents('Print');

			texts.children().each((i, el) => {
				$('input', el).val(randExp.gen());
			});
			updateRegex(regex);
			settings.lastRegex = regex;
			if (settings.shareRegex) {
				$.post('/lastUsedRegex', { regex: regex });
			}
		} catch (e) {
			playAgents('GetAttention');
			regexp.addClass('is-invalid');
		}
	}
	
	let updateRegexList = () => {
		regexList.children().remove();
		let stored = settings.storedRegex;
		$.each(stored, (k) => {
			regexList.append(templates.regexListItem({regex: k}));
		});
	}
	
	let updateLastUsedRegex = (regexStats) => {
		if (regexStats) {
			lastUsedRegexInput.val(regexStats.regex).attr({'data-original-title': 'Use ' + regexStats.regex}).tooltip();
			$('span', lastUsedRegexPopupToggler).text(regexStats.used);
			let ago = moment(regexStats.lastUsed).fromNow();
			lastUsedRegexPopupToggler.attr({'data-original-title': ago, 'data-last-used': regexStats.lastUsed}).tooltip();
		} else {
			lastUsedRegexDd.val('');
			$('span', lastUsedRegexPopupToggler).text(0);
		}
	}
	
	// events
	$(document.getElementById('create_new')).on('click', generateTexts);
	
	$(document.getElementById('use_default')).on('click', (e) => {
		e.preventDefault();
		updateRegex(defaults.regex);
	});
	
	$(document.getElementById('reset_regex')).on('click', (e) => {
		e.preventDefault();
		regexp.removeClass('is-invalid');
		updateRegex(settings.lastRegex);
	});
	
	$(texts).on('click', 'button', (e) => {
		e.preventDefault();
		let dummy = document.createElement('input');
		document.body.appendChild(dummy);
		let button = $(e.currentTarget);
		dummy.value = button.parent().siblings('input').val();
		dummy.select();
		document.execCommand('copy');
		document.body.removeChild(dummy);
		$('i', button).removeClass('fa-clipboard').addClass('fa-clipboard-check');
		setTimeout(() => { $('i', button).removeClass('fa-clipboard-check').addClass('fa-clipboard') }, 500);
	});
	
	$(document.getElementById('save_local')).on('click', (e) => {
		e.preventDefault();
		let stored = settings.storedRegex;
		let currentValue = regexp.val();
		if (!currentValue) {
			return;
		}
		stored[currentValue] = (stored[currentValue] || 0) + 1;
		settings.storedRegex = stored;
		updateRegexList();
	});
	
	regexList.on('click', 'a', (e) => {
		e.preventDefault();
		regexp.val($(e.currentTarget).text());
		regexDialog.modal('hide');
	});
	
	regexList.on('click', 'button', (e) => {
		e.preventDefault();
		let regexToDelete = $(e.currentTarget).parent().siblings('a').text();
		let stored = settings.storedRegex;
		delete stored[regexToDelete];
		settings.storedRegex = stored;
		updateRegexList();
	})
	
	$(document.getElementById('add_row')).on('click', (e) => {
		e.preventDefault();
		let row = $(templates.texts());
		texts.append(row);
		$('[data-toggle="tooltip"]', row).tooltip();
		$('input', row).val(new RandExp(settings.lastRegex).gen());
		settings.numTexts = settings.numTexts + 1;
	});
	
	$(document.getElementById('remove_row')).on('click', (e) => {
		e.preventDefault();
		if (settings.numTexts <= 1) {
			return;
		}
		texts.children().last().remove();
		settings.numTexts = settings.numTexts - 1;
	});
	
	shareRegexCheck.on('change', (e) => {
		settings.shareRegex = shareRegexCheck.prop('checked');
	});
	
	lastUsedRegexInput.on('click', (e) => {
		updateRegex($(e.currentTarget).val());
	});
	
	let dialogFromNowTimer;
	
	regexStatsDialog.on('show.bs.modal', () => {
		let statItems = [];
		$.get('mostUsedRegex', (data) => {
			regexStatsList.children().remove();
			$.each(data, (i, el) => {
				let statItem = $(templates.regexStatsItem(el));
				statItems.push(statItem);
				regexStatsList.append(statItem);
				statItem.attr({
					'data-original-title': moment(el.lastUsed).fromNow(),
					'data-last-used': el.lastUsed
				}).tooltip();
			});
		});
		
		dialogFromNowTimer = setInterval(() => {
			$.each(statItems, (i, el) => {
				el.attr('data-original-title', moment(el.attr('data-last-used')).fromNow());
			});
		}, 100);
	});
	
	regexStatsDialog.on('hide.bs.modal', () => {
		if (dialogFromNowTimer) {
			clearInterval(dialogFromNowTimer);
		}
	});
	
	regexStatsList.on('click', 'a', (e) => {
			e.preventDefault();
			updateRegex($(e.currentTarget).data('regex'));
			regexStatsDialog.modal('hide');
	});
	
	// timers
	setInterval(() => {
		if (!lastUsedRegexPopupToggler.attr('data-last-used')) {
			return;
		}
		let ago = moment(lastUsedRegexPopupToggler.attr('data-last-used')).fromNow();
		lastUsedRegexPopupToggler.attr('data-original-title', ago).tooltip();
	}, 100);
	
	// initializing
	(function() {
		let socket = new SockJS('/raasWs');
		stompClient = Stomp.over(socket);
		stompClient.debug = null;
		stompClient.connect({}, () => {
			stompClient.subscribe('/topic/lastUsedRegex', (data) => {
				updateLastUsedRegex(JSON.parse(data.body));
			});
		});
	}());
	
	updateRegex(settings.lastRegex);
	initTexts();
	generateTexts();
	updateRegexList();
	shareRegexCheck.prop('checked', settings.shareRegex).parent().toggleClass('active', settings.shareRegex);
	shareRegexCheck.siblings('a').on('click', (e) => { e.stopPropagation() });
	
	$.get('lastUsedRegex', updateLastUsedRegex);
	
	$('[data-toggle="tooltip"]').tooltip();
	$('[data-toggle="popover"]').popover();
	$('form').on('submit', (e) => {
		e.preventDefault();
		generateTexts();
	});
	
	let randPos = () => .2 + Math.random() * .6;
	let toggleAgent = (name) => {
		if (agents[name].visible) {
			agents[name].agent.stop();
			agents[name].visible = false;
			agents[name].agent.hide(true);
			return;
		}
		if (!agents[name].agent) {
			clippy.load(name, (agent) => {
				agents[name].agent = agent;
				agents[name].agent.show();
				agents[name].agent.moveTo($(document).width() / 2 * randPos(), $(document).height() * randPos());
				agents[name].agent.play('Wave');
			});
		} else {
			agents[name].agent.stop();
			agents[name].agent.show();
			agents[name].agent.play('Wave');
		}
		agents[name].visible = true;
	}
	
	$.each(settings.agents, (i, name) => {
		toggleAgent(name);
	});
	
	
	$.each(agents, (k, v) => {
		let agentDom = $(templates.agent({name: k})).toggleClass('active', v.visible).on('click', (e) => {
			e.preventDefault();
			e.stopPropagation();
			toggleAgent(k);
			agentDom.toggleClass('active', v.visible);
			let visibleAgents = settings.agents;
			if (v.visible) {
				visibleAgents.push(k);
			} else {
				let index = visibleAgents.indexOf(k);
				if (index != -1) {
					visibleAgents.splice(index, 1);
				}
			}
			settings.agents = visibleAgents;
		});
		agentDropdown.append(agentDom);
	});
	
}(jQuery));