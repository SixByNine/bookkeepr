function update(){
	new Ajax.PeriodicalUpdater('status', '/ident',
			{
method: 'get',
frequency: 5,
decay: 2
});


}
