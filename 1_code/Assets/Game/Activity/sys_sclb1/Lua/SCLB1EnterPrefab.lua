local basefunc = require "Game/Common/basefunc"
SCLB1EnterPrefab = basefunc.class()
local C = SCLB1EnterPrefab
C.name = "SCLB1EnterPrefab"

function C.Create(parent, cfg)
	return C.New(parent, cfg)
end

function C:AddMsgListener()
    for proto_name,func in pairs(self.lister) do
        Event.AddListener(proto_name, func)
    end
end

function C:MakeLister()
    self.lister = {}
    self.lister["model_sclb1_gift_change_msg"] = basefunc.handler(self, self.on_model_sclb1_gift_change_msg)
end

function C:RemoveListener()
    for proto_name,func in pairs(self.lister) do
        Event.RemoveListener(proto_name, func)
    end
    self.lister = {}
end

function C:MyExit()
	if self.update_timer then
        self.update_timer:Stop()
	end
	self.update_timer = nil
	self:RemoveListener()
	destroy(self.gameObject)
end

function C:ctor(parent, cfg)
	self.config = cfg
	local obj = newObject("sclb1_btn", parent)
	local tran = obj.transform
	self.transform = tran
	self.gameObject = obj
	LuaHelper.GeneratingVar(self.transform, self)
	self:MakeLister()
	self:AddMsgListener()
	self.transform.localPosition = Vector3.zero
	self:InitUI()
end

function C:InitUI()
	self.enter_btn = self.transform:GetComponent("Button")
	self.enter_btn.onClick:AddListener(function ()
    	ExtendSoundManager.PlaySound(audio_config.game.com_but_confirm.audio_name)
		self:OnEnterClick()
	end)

	self:MyRefresh()
end

function C:MyRefresh()
	if self.update_timer then
        self.update_timer:Stop()
	end
	self.update_timer = nil
	self.time = SYSSCLB1Manager.GetOutTime()
	self.update_timer = Timer.New(function()
        self.time = self.time - 1
        self:UpdateTime()
    end, 1, -1, nil, true)
    self.update_timer:Start()
    self:UpdateTime()
	self:RefreshStages()
end

function C:RefreshStages()
	if SYSSCLB1Manager.check_can_pay() then
		return
	end
    Event.Brocast("ui_button_state_change_msg")

    self:OnDestroy()
end

function C:OnEnterClick()
	SCLB1Panel.Create()
end

function C:OnDestroy()
	self:MyExit()
end

function C:on_model_sclb1_gift_change_msg()
	self:MyRefresh()
end

function C:UpdateTime()
    if self.time<=0 then
        self.time=0
    end 
	local str 
	str = StringHelper.formatTimeDHMS3(self.time)
	if IsEquals(self.time_txt) then
		self.time_txt.text = str
	end
	if self.time <= 0 then
		if self.update_timer then
			self.update_timer:Stop()
		end
        self.update_timer = nil
	end
end

function C:ToTimeStr(second)
	if not second or second < 0 then
        return "0???"
    end
    local timeDay = math.floor(second/86400)
    local timeHour = math.fmod(math.floor(second/3600), 24)
    local timeMinute = math.fmod(math.floor(second/60), 60)
    local timeSecond = math.fmod(second, 60)
    if timeDay > 0 then
        return string.format("%d???%d???%d???", timeDay, timeHour, timeMinute)
    elseif timeHour > 0 then
        return string.format("%d???%d???", timeHour, timeMinute)
    elseif timeMinute > 0 then
        return string.format("%d???", timeMinute)
    else
        return string.format("%d???", timeSecond)
    end
end