export default class SearchableDropdown {
  constructor({
                element,
                items = [],
                placeholder = 'Search...'
              }) {
    if (!element) {
      throw new Error('SearchableDropdown: element is required');
    }

    this.originalElement =
        typeof element === 'string'
            ? document.querySelector(element)
            : element;

    if (!this.originalElement) {
      throw new Error('SearchableDropdown: element not found');
    }

    this.isSelect = this.originalElement.tagName === 'SELECT';

    this.items = this.isSelect
        ? this._extractItemsFromSelect(this.originalElement)
        : items;

    this.placeholder = placeholder;

    this._createContainer();
    this._render();
    this._bindEvents();
  }

  /* ---------- Initialization ---------- */

  _extractItemsFromSelect(select) {
    return Array.from(select.options).map(option => ({
      value: option.value,
      label: option.text
    }));
  }

  _createContainer() {
    this.container = document.createElement('div');
    this.container.className = 'searchable-dropdown';

    this.originalElement.parentNode.insertBefore(
        this.container,
        this.originalElement.nextSibling
    );

    if (this.isSelect) {
      this.originalElement.style.display = 'none';
    }
  }

  _render() {
    this.input = document.createElement('input');
    this.input.type = 'text';
    this.input.placeholder = this.placeholder;

    this.optionsEl = document.createElement('div');
    this.optionsEl.className = 'searchable-dropdown-options';
    this.optionsEl.style.display = 'none';

    this.container.appendChild(this.input);
    this.container.appendChild(this.optionsEl);

    if (this.isSelect && this.originalElement.value) {
      const selected = this.items.find(
          item => item.value === this.originalElement.value
      );
      if (selected) {
        this.input.value = selected.label;
      }
    }
  }

  /* ---------- Events ---------- */

  _bindEvents() {
    this.input.addEventListener('input', () => {
      const query = this.input.value.toLowerCase();
      const filtered = this.items.filter(item =>
          item.label.toLowerCase().includes(query)
      );
      this._renderOptions(filtered);
      this._show();
    });

    this.input.addEventListener('focus', () => {
      this._renderOptions(this.items);
      this._show();
    });

    document.addEventListener('click', e => {
      if (!this.container.contains(e.target)) {
        this._hide();
      }
    });

    window.addEventListener('scroll', () => {
      if (!this.container.contains(e.target)) {
        this._hide();
      }
    }, true);
    window.addEventListener('resize', () => this._hide());

  }

  /* ---------- Rendering ---------- */

  _renderOptions(list) {
    this.optionsEl.innerHTML = '';

    list.forEach(item => {
      const option = document.createElement('div');
      option.className = 'option';
      option.textContent = item.label;

      option.addEventListener('click', () => {
        this.selectItem(item);
      });

      this.optionsEl.appendChild(option);
    });
  }

  _show() {
    const rect = this.input.getBoundingClientRect();

    Object.assign(this.optionsEl.style, {
      position: 'absolute',
      top: `${rect.bottom + window.scrollY + 2}px`,
      left: `${rect.left + window.scrollX}px`,
      width: `auto`,
      display: 'block',
      ...this.style,
    });

    document.body.appendChild(this.optionsEl);
    this.container.classList.add('open');
  }

  _hide() {
    this.optionsEl.style.display = 'none';
    this.container.classList.remove('open');
    this.input.blur();

    if (this.container.contains(this.optionsEl) === false) {
      this.container.appendChild(this.optionsEl);
    }
  }

  /* ---------- Public API ---------- */

  selectItem(item) {
    this.input.value = item ? item.label : "";
    this._hide();

    if (this.isSelect) {
      this.originalElement.value = item ? item.value : null;
      this.originalElement.dispatchEvent(
          new Event('change', { bubbles: true })
      );
    }
  }

  get value() {
    return this.isSelect
        ? this.originalElement.value
        : this.input.value;
  }

  set value(val) {
    if (this.isSelect) {
      const item = this.items.find(i => i.value === val);
      if (item) {
        this.originalElement.value = val;
        this.input.value = item.label;
      }
    } else {
      this.input.value = val;
    }
  }
}
