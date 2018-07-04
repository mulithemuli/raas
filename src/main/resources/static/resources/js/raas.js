(function($) {
	let defaults = {
			regex: '([a-zA-Z0-9_@:!]){4}([a-zA-Z]){2}([0-9]){2}',
			numTexts: 5
	}
	
	// fields / elements
	let regexp = $(document.getElementById('regexp'));
	let texts = $(document.getElementById('texts'));
	
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
</div>')
	};
	
	// functions
	let updateRegex = (ex) => {
		regexp.val(ex);
	}
	
	let lastRegex = () => {
		return localStorage.getItem('last_regex') || defaults.regex;
	}
	
	let numTexts = () => {
		return parseInt(localStorage.getItem('num_texts') || defaults.numTexts, 10);
	}
	
	let storedRegex = () => {
		return JSON.parse(localStorage.getItem('stored_regex') || '{}');
	}
	
	let initTexts = () => {
		let count = numTexts();
		for (let i = 0; i < count; i++) {
			texts.append(templates.texts());
		}
	}
	
	let generateTexts = (e) => {
		if (e) {
			e.preventDefault();
		}
		regexp.removeClass('is-invalid');
		let regex = regexp.val() || lastRegex();
		try {
			let randExp = new RandExp(regex);
			texts.children().each((i, el) => {
				$('input', el).val(randExp.gen());
			});
			updateRegex(regex);
			localStorage.setItem('last_regex', regex);
		} catch (e) {
			regexp.addClass('is-invalid');
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
		updateRegex(lastRegex);
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
		let stored = storedRegex();
		let currentValue = regexp.val();
		if (!currentValue) {
			return;
		}
		stored[currentValue] = (stored[currentValue] || 0) + 1;
		localStorage.setItem('stored_regex', JSON.stringify(stored));
	});
	
	$(document.getElementById('open_local')).on('click', (e) => {
		e.preventDefault();
		let stored = storedRegex();
		$.each(stored, (k, v) => {
			console.log(k, v);
			
		});
	});
	
	$(document.getElementById('add_row')).on('click', (e) => {
		e.preventDefault();
		let row = $(templates.texts());
		texts.append(row);
		$('input', row).val(new RandExp(lastRegex()).gen());
		localStorage.setItem('num_texts', numTexts() + 1);
	});
	
	$(document.getElementById('remove_row')).on('click', (e) => {
		e.preventDefault();
		if (numTexts() <= 1) {
			return;
		}
		texts.children().last().remove();
		localStorage.setItem('num_texts', numTexts() - 1);
	});
	
	// initializing
	updateRegex(lastRegex());
	initTexts();
	generateTexts();
	
	$('[data-toggle="tooltip"]').tooltip();
	$('form').on('submit', (e) => {
		e.preventDefault();
		generateTexts();
	});
}(jQuery));